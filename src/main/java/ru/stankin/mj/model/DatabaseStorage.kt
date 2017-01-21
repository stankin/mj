package ru.stankin.mj.model


import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.support.jdk8.collections.stream
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.sql2o.Connection
import org.sql2o.Query
import org.sql2o.ResultSetIterable
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction

import javax.enterprise.inject.Default
import javax.inject.Inject
import javax.inject.Singleton

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Created by nickl on 01.02.15.
 */
//@javax.ejb.Singleton
//@javax.ejb.Startup
@Singleton
//@javax.inject.Singleton
@Default
//@Lock(LockType.READ)
class DatabaseStorage
@Inject
constructor(private val sql2o: Sql2o) {

    private val logger = LogManager.getLogger(DatabaseStorage::class.java)

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
                    .map { loadSubject(it) }
                    .collect(Collectors.toList<Module>())

            var added = 0
            var updated = 0
            var deleted = 0

            for (module in studentModules) {
                module.subject = persistedSubject(module.subject)

                val existigModule = currentModules.stream().filter({ cur -> cur.getSubject() == module.subject && cur.getNum() == module.num }).findAny()

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
                            .addParameter("persisted", module.getSubject().getId())
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

    fun deleteStudent(s: Student) {
        logger.debug("deleting student {}", s)
        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("DELETE FROM users WHERE id=:id;").addParameter("id", s.id).executeUpdate()
            connection.commit()
        }
    }

    fun saveStudent(student: Student, semestr: String?) {

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->

            logger.trace("saving student {} at semester {}", student, semestr)

            if (student.id == 0) {
                logger.trace("inserting student {}", student)
                val userId = connection
                        .createQuery("INSERT INTO users (login, initials, name, patronym, surname) " + "VALUES (:cardid, :initials, :name, :patronym, :surname)", true)
                        .bind(student)
                        .executeUpdate().getKey<Int>(Int::class.java)

                student.id = userId!!

                connection.createQuery("INSERT INTO student (id, stgroup) " + "VALUES (:id, :stgroup)")
                        .bind(student)
                        .executeUpdate()
            } else {
                logger.trace("updating student {}", student)
                connection
                        .createQuery("UPDATE users  SET login = :cardid," +
                                " initials = :initials," +
                                " name = :name," +
                                " patronym = :patronym," +
                                " surname = :surname" +
                                " WHERE id = :id")
                        .bind(student)
                        .executeUpdate()

                connection.createQuery("UPDATE student SET stgroup = :stgroup " + "WHERE id = :id")
                        .bind(student)
                        .executeUpdate()
            }

            if(semestr != null) {

                logger.trace("updating student groups history {} at semester {}", student, semestr)

                val group = connection
                        .createQuery("SELECT * FROM groupshistory WHERE student_id = :studentId AND semestr = :semester LIMIT 1")
                        .addParameter("studentId", student.id)
                        .addParameter("semester", semestr)
                        .throwOnMappingFailure(false)
                        .executeAndFetchFirst(StudentHistoricalGroup::class.java)

                logger.trace("updating student group at semester {} {}", semestr, group)

                if (group == null) {
                    connection
                            .createQuery("INSERT INTO groupshistory (groupname, semestr, student_id) " + "VALUES (:group, :semester, :student)")
                            .addParameter("group", student.stgroup)
                            .addParameter("semester", semestr)
                            .addParameter("student", student.id)
                            .executeUpdate()
                } else if (group.groupName != student.stgroup) {
                    connection
                            .createQuery("UPDATE groupshistory SET groupname = :group WHERE id = :entryid")
                            .addParameter("group", student.stgroup)
                            .addParameter("entryid", group.id)
                            .executeUpdate()
                }
            }
            connection.commit()
        }
    }


    val students: Stream<Student>
        get() = getStudentsFiltred("")

    fun getStudentsFiltred(text: String?): Stream<Student> {
        logger.debug("getStudentsFiltred '{}'", text)
        if (text == null)
            return students

        val connection = sql2o.open(ThreadLocalTransaction.get())
        try {
            return toStream(connection
                    .createQuery("SELECT users.id AS id, users.login AS cardid, * FROM users INNER JOIN student ON users.id = student.id  WHERE surname || initials || stgroup || users.login ILIKE :pattern ORDER BY stgroup, surname;\n")
                    .addParameter("pattern", "%$text%")
                    .throwOnMappingFailure(false)
                    .executeAndFetchLazy(Student::class.java), connection)

        } catch (e: Exception) {
            connection.close()
            throw e
        }

    }


    fun getStudentById(id: Int, semester: String?): Student? {

        sql2o.open().use { connection ->
            val student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" + " WHERE users.id = :id")
                    .addParameter("id", id)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student::class.java)

            if (student != null) {
                student.groups = getStudentHistoricalGroups(connection, id)
                if (semester != null) {
                    student.modules = getStudentModules(connection, semester, id)
                }
            }

            return student
        }
    }

    private fun getStudentModules(connection: Connection, semester: String, studentid: Int): List<Module> {
        return connection
                .createQuery("SELECT *, student_id as studentId, subject_id as subjectId from modules WHERE student_id = :id and subject_id in (SELECT subjects.id from subjects WHERE semester = :semester)")
                .addParameter("id", studentid)
                .addParameter("semester", semester)
                .throwOnMappingFailure(false)
                .executeAndFetch(Module::class.java).stream()
                .map({ loadSubject(it) }).collect(Collectors.toList<Module>())
    }

    private fun getStudentHistoricalGroups(connection: Connection, studentid: Int): List<StudentHistoricalGroup> {
        return connection
                .createQuery("SELECT * FROM groupshistory WHERE student_id = :id")
                .addParameter("id", studentid)
                .throwOnMappingFailure(false)
                .executeAndFetch(StudentHistoricalGroup::class.java)
    }

    fun getStudentSemestersWithMarks(student: Int): Set<String> {
        sql2o.open().use { connection ->
            return connection.createQuery("SELECT DISTINCT semester FROM subjects WHERE subjects.id in (SELECT DISTINCT subject_id FROM modules WHERE student_id = :studentId)")
                    .addParameter("studentId", student)
                    .executeScalarList(String::class.java)
                    .stream()
                    .filter( { Objects.nonNull(it) })
                    .collect(Collectors.toCollection( { TreeSet<String>() }))
        }
    }

    val knownSemesters: Set<String>
        get() = sql2o.open().use { connection ->
            return connection.createQuery("SELECT DISTINCT semester FROM subjects WHERE subjects.id in (SELECT DISTINCT subject_id FROM modules)")
                    .executeScalarList(String::class.java)
                    .stream()
                    .filter( { Objects.nonNull(it) })
                    .collect(Collectors.toCollection( { TreeSet<String>() }))
        }

    private fun getOrCreateSubject(subjData: SubjData): Subject {

        sql2o.beginTransaction().use { connection ->
            val subject = connection.createQuery("SELECT * FROM subjects WHERE semester = :semester AND stgroup = :group AND subjects.title = :title LIMIT 1")
                    .addParameter("semester", subjData.semester)
                    .addParameter("group", subjData.group)
                    .addParameter("title", subjData.name)
                    .executeAndFetchFirst(Subject::class.java)
            if (subject != null && Math.abs(subject.factor - subjData.factor) < 0.001) {
                return subject
            } else if (subject == null) {
                val id = connection
                        .createQuery("INSERT INTO subjects (factor, stgroup, title, semester) VALUES (:factor, :group, :title, :semester)")
                        .addParameter("semester", subjData.semester)
                        .addParameter("group", subjData.group)
                        .addParameter("title", subjData.name)
                        .addParameter("factor", subjData.factor)
                        .executeUpdate().getKey<Int>(Int::class.java)
                connection.commit()
                return Subject(id, subjData.semester, subjData.group, subjData.name, subjData.factor)
            } else {
                connection
                        .createQuery("UPDATE subjects SET factor = :factor WHERE id = :id")
                        .addParameter("factor", subjData.factor)
                        .addParameter("id", subject.id)
                        .executeUpdate()
                subject.factor = subjData.factor
                connection.commit()
                return subject
            }
        }
    }

    fun getStudentByGroupSurnameInitials(semester: String?, group: String, surname: String, initials: String): Student? {

        sql2o.open().use { connection ->
            val student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" + " WHERE student.stgroup = :group and users.surname = :surname AND users.initials = :initials")
                    .addParameter("group", group)
                    .addParameter("surname", surname)
                    .addParameter("initials", initials)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student::class.java)

            if (student != null) {
                student.groups = getStudentHistoricalGroups(connection, student.id)
                if (semester != null) {
                    student.modules = getStudentModules(connection, semester, student.id)
                }
            }

            return student
        }
    }


    fun getStudentByCardId(cardid: String): Student? {

        sql2o.open(ThreadLocalTransaction.get()).use { connection ->
            val student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" + " WHERE login = :login")
                    .addParameter("login", cardid)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student::class.java)

            if (student != null)
                student.groups = getStudentHistoricalGroups(connection, student.id)

            return student
        }
    }

    private val subjectsCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build<Int, Subject>(CacheLoader.from { id ->
                sql2o.open(ThreadLocalTransaction.get()).use { connection ->
                    connection
                            .createQuery("SELECT * FROM subjects WHERE subjects.id = :id")
                            .addParameter("id", id)
                            .throwOnMappingFailure(false)
                            .executeAndFetchFirst(Subject::class.java) ?: throw NoSuchElementException("no subject for id ${id}")

                }
            })

    private fun loadSubject(module: Module) = module.apply { subject = subjectsCacheById.get(this.subjectId) }

    private data class SubjData(val semester: String, val group: String, val name: String, val factor: Double)

    private val subjectsCacheBySubjData = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build<SubjData, Subject>(CacheLoader.from { inf ->  getOrCreateSubject(inf!!)})

    private fun persistedSubject(un: Subject) = subjectsCacheBySubjData.get(SubjData(un.semester, un.stgroup, un.title, un.factor))


    private fun <T> toStream(resultSet: ResultSetIterable<T>, connection: Connection): Stream<T> {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resultSet.iterator(),
                Spliterator.DISTINCT or Spliterator.NONNULL or
                        Spliterator.CONCURRENT or Spliterator.IMMUTABLE
        ), false).onClose {
            resultSet.close()
            connection.close()
        }
    }

}
