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
import ru.stankin.mj.utils.toStream

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
class StudentsStorage
@Inject
constructor(private val sql2o: Sql2o, private val modules: ModulesStorage) {

    private val logger = LogManager.getLogger(StudentsStorage::class.java)

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


    fun getStudentById(id: Int, semester: String?): Student? =
            ThreadLocalTransaction.joinOrNew(sql2o) {
                sql2o.open(ThreadLocalTransaction.get()).use { connection ->
                    val student = connection
                            .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" + " WHERE users.id = :id")
                            .addParameter("id", id)
                            .throwOnMappingFailure(false)
                            .executeAndFetchFirst(Student::class.java)

                    if (student != null) {
                        student.groups = getStudentHistoricalGroups(connection, id)
                        if (semester != null) {
                            student.modules = modules.getStudentModules(semester, id)
                        }
                    }
                    student
                }
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


    fun getStudentByGroupSurnameInitials(semester: String?, group: String, surname: String, initials: String): Student? =
            ThreadLocalTransaction.joinOrNew(sql2o) {
                sql2o.open(ThreadLocalTransaction.get()).use { connection ->
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
                            student.modules = modules.getStudentModules(semester, student.id)
                        }
                    }

                    student
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


}
