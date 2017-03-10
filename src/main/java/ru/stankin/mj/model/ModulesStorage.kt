package ru.stankin.mj.model

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.support.jdk8.collections.stream
import org.apache.logging.log4j.LogManager
import org.sql2o.Connection
import org.sql2o.ResultSetIterable
import org.sql2o.Sql2o
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.utils.ThreadLocalTransaction
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.enterprise.inject.Default
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by nickl on 22.01.17.
 */


@Singleton
class ModulesStorage @Inject constructor(private val sql2o: Sql2o, private val subjects: SubjectsStorage) {

    private val logger = LogManager.getLogger(ModulesStorage::class.java)

    fun updateModules(student: Student): ModulesUpdateStat {

        if (student.modules.isEmpty())
            throw IllegalArgumentException("Student:" + student.cardid + " has no modules")

        val studentModules = ArrayList(student.modules)
        val semester = studentModules[0].subject.semester
        logger.debug("saving student {} at {} modules: {}", student.cardid, semester, studentModules.size)

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->

            val currentModules = connection
                    .createQuery("select *, student_id as studentId, subject_id as subjectId from modules JOIN subjects ON modules.subject_id = subjects.id WHERE student_id = :id AND semester = :semester")
                    .addParameter("id", student.id)
                    .addParameter("semester", semester)
                    .throwOnMappingFailure(false)
                    .executeAndFetch(Module::class.java).stream()
                    .map { subjects.loadSubject(it) }
                    .collect(Collectors.toList<Module>())

            var added = 0
            var updated = 0
            var deleted = 0

            for (module in studentModules) {
                module.subject = subjects.persistedSubject(module.subject)

                val existigModule = currentModules.stream().filter({ cur -> cur.getSubject().id == module.subject.id && cur.getNum() == module.num }).findAny()

                existigModule.ifPresent{ currentModules.remove(it) }

                if (existigModule.isPresent() && existigModule.get() != module) {

                    connection
                            .createQuery("UPDATE modules SET color = :color, value = :value, transaction = :transaction WHERE student_id = :student AND subject_id = :subject AND num = :num")
                            .addParameter("color", module.color)
                            .addParameter("value", module.value)
                            .addParameter("student", existigModule.get().studentId)
                            .addParameter("subject", existigModule.get().getSubject().getId())
                            .addParameter("transaction", ModifyingTransactions.modifyingTransaction().id)
                            .addParameter("num", existigModule.get().getNum())
                            .executeUpdate()
                    updated++
                } else if (!existigModule.isPresent()) {
                    connection
                            .createQuery("INSERT INTO modules (color, num, value, student_id, subject_id, transaction) VALUES (:color, :num, :value, :studentId, :subjectId, :transaction)")
                            .bind(module)
                            .addParameter("studentId", student.id)
                            .addParameter("subjectId", module.subject.id)
                            .addParameter("transaction", ModifyingTransactions.modifyingTransaction().id)
                            .executeUpdate()
                    added++
                }

            }

            if (!currentModules.isEmpty()) {

                val query = connection.createQuery("DELETE FROM modules WHERE student_id = :student AND subject_id = :subject AND num = :num")

                for (module in currentModules) {
                    query
                            .addParameter("student", module.studentId)
                            .addParameter("subject", module.getSubject().getId())
                            .addParameter("num", module.getNum())
                            .addToBatch()
                }
                query.executeBatch()

                val query1 = connection
                        .createQuery("INSERT INTO moduleshistory (color, num, value, student_id, subject_id, transaction)  VALUES (0,:num, -1, :studentId, :subjectId, :transaction)")

                for (module in currentModules) {
                    query1
                            .addParameter("num", module.getNum())
                            .addParameter("studentId", student.id)
                            .addParameter("subjectId", module.subject.id)
                            .addParameter("transaction", ModifyingTransactions.modifyingTransaction().id)
                            .addToBatch()
                }
                query1.executeBatch()

                deleted += currentModules.size
            }

            connection.commit()
            return ModulesUpdateStat(added, updated, deleted)
        }

    }

    fun deleteAllModules(semester: String) {
        ThreadLocalTransaction.joinOrNew(sql2o) {
            sql2o.beginTransaction(ThreadLocalTransaction.get()).setRollbackOnClose(false).use { connection ->

                connection
                        .createQuery("""INSERT INTO moduleshistory (color, num, value, student_id, subject_id, transaction)
                                    SELECT 0, num, -1, student_id, subject_id, :transaction FROM modules WHERE subject_id IN
                                    (SELECT subjects.id FROM subjects WHERE semester = :semester)""".trimIndent())
                        .addParameter("semester", semester)
                        .addParameter("transaction", ModifyingTransactions.modifyingTransaction().id)
                        .executeUpdate()

                connection
                        .createQuery("DELETE FROM modules WHERE subject_id IN (SELECT subjects.id FROM subjects WHERE semester = :semester)")
                        .addParameter("semester", semester)
                        .executeUpdate()
                connection.commit()
            }
        }
    }

    fun getStudentModules(semester: String, studentid: Int): List<Module> {
        return sql2o.open(ThreadLocalTransaction.get()).use {  it
                .createQuery("SELECT *, student_id as studentId, subject_id as subjectId from modules WHERE student_id = :id and subject_id in (SELECT subjects.id from subjects WHERE semester = :semester)")
                .addParameter("id", studentid)
                .addParameter("semester", semester)
                .throwOnMappingFailure(false)
                .executeAndFetch(Module::class.java).stream()
                .map({ subjects.loadSubject(it) }).collect(Collectors.toList<Module>()) }
    }


    fun getStudentModulesChanges(semester: String, studentid: Int): Map<Transaction, List<Module>> =
            sql2o.open(ThreadLocalTransaction.get()).use {

                it.createQuery("""WITH subjs AS (SELECT id FROM subjects WHERE semester = :semester)
                                  (SELECT student_id, num, value, color, subject_id, transaction FROM modules WHERE student_id = :id AND subject_id IN (SELECT * FROM subjs))
                                  UNION ALL
                                  (SELECT student_id, num, value, color, subject_id, transaction FROM moduleshistory WHERE student_id = :id AND subject_id IN (SELECT * FROM subjs)) ORDER BY transaction DESC
                                  """.trimIndent())
                        .addParameter("id", studentid)
                        .addParameter("semester", semester)
                        .throwOnMappingFailure(false)
                        .executeAndFetch(HistoryRecord::class.java).map {
                    hr ->
                    transactionsCacheById.get(hr.transaction) to Module(subjects.byId(hr.subject_id), studentid, hr.num).apply {
                        color = hr.color
                        value = hr.value
                    }
                }.groupBy({ it.first }, { it.second })
            }

    data class HistoryRecord(val student_id: String, val num: String, val value: Int, val color: Int, val subject_id: Int, val transaction: Long)


    private val transactionsCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build<Long, Transaction>(CacheLoader.from { id ->
                sql2o.open(ThreadLocalTransaction.get()).use { connection ->
                    connection
                            .createQuery("SELECT t.id, time AS date, users.login AS authorLogin  FROM transactions t JOIN users ON t.author = users.id WHERE t.id = :id")
                            .addParameter("id", id)
                            .throwOnMappingFailure(false)
                            .executeAndFetchFirst(Transaction::class.java) ?: throw NoSuchElementException("no transaction for id ${id}")

                }
            })

}

data class Transaction(val id: Long, val date: Date, val authorLogin: String)

object ModifyingTransactions {

    private val transactions = CacheBuilder.newBuilder()
            .weakKeys()
            .build<Connection, Transaction>(CacheLoader.from { connection ->

                val user = MjRoles.getUser()!!
                val time = Date()

                val trId = connection!!
                        .createQuery("INSERT INTO transactions (time, author) VALUES (:time, :author)")
                        .addParameter("time", time)
                        .addParameter("author", user.id)
                        .executeUpdate().getKey<Long>(Long::class.java)


                Transaction(trId, time, user.username)
            })

    @JvmStatic fun modifyingTransaction(): Transaction {
        val connection = ThreadLocalTransaction.getSql2oConnection() ?: throw IllegalStateException("not in transaction")
        return transactions.get(connection)
    }
}