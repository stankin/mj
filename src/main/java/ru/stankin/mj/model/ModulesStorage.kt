package ru.stankin.mj.model

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.support.jdk8.collections.stream
import org.apache.logging.log4j.LogManager
import org.sql2o.Connection
import org.sql2o.ResultSetIterable
import org.sql2o.Sql2o
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
                            .createQuery("UPDATE modules SET color = :color, value = :value " + "WHERE student_id = :student AND subject_id = :subject AND num = :num")
                            .addParameter("color", module.color)
                            .addParameter("value", module.value)
                            .addParameter("student", existigModule.get().studentId)
                            .addParameter("subject", existigModule.get().getSubject().getId())
                            .addParameter("num", existigModule.get().getNum())
                            .executeUpdate()
                    updated++
                } else if (!existigModule.isPresent()) {
                    connection
                            .createQuery("INSERT INTO modules (color, num, value, student_id, subject_id) VALUES (" + ":color, :num, :value, :studentId, :subjectId)")
                            .bind(module)
                            .addParameter("studentId", student.id)
                            .addParameter("subjectId", module.subject.id)
                            .executeUpdate()
                    added++
                }


            }

            if (!currentModules.isEmpty()) {
                logger.debug("removing modules {}", currentModules)

                val query = connection.createQuery("DELETE FROM modules " + "WHERE student_id = :student AND subject_id = :subject AND num = :num")

                for (module in currentModules) {
                    query.addParameter("student", module.studentId)
                            .addParameter("subject", module.getSubject().getId())
                            .addParameter("num", module.getNum())
                            .addToBatch()
                }

                query.executeUpdate()
                deleted += currentModules.size
            }

            connection.commit()
            val updateStat = ModulesUpdateStat(added, updated, deleted)
            logger.debug("update stat: " + updateStat)
            return updateStat
        }

    }

    fun deleteStudentModules(student: Student, semester: String) {

        sql2o.beginTransaction().setRollbackOnClose(false).use { connection ->

            connection
                    .createQuery("DELETE FROM modules WHERE student.id = :studentid && subject_id in (SELECT subjects.id FROM subjects WHERE semester = :semester)")
                    .addParameter("studentid", student.id)
                    .addParameter("semester", semester)
                    .executeUpdate()
        }

    }

    fun deleteAllModules(semester: String) {
        sql2o.beginTransaction().setRollbackOnClose(false).use { connection ->

            connection
                    .createQuery("DELETE FROM modules WHERE subject_id in (SELECT subjects.id FROM subjects WHERE semester = :semester)")
                    .addParameter("semester", semester)
                    .executeUpdate()
            connection.commit()
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




}