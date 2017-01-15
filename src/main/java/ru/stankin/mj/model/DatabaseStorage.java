package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.sql2o.Connection;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;
import ru.stankin.mj.utils.ThreadLocalTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by nickl on 01.02.15.
 */
//@javax.ejb.Singleton
//@javax.ejb.Startup
@Singleton
//@javax.inject.Singleton
@Default
//@Lock(LockType.READ)
public class DatabaseStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(DatabaseStorage.class);

    private final
    Sql2o sql2o;

    @Inject
    public DatabaseStorage(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @Override
    public void updateModules(Student student0) {

        Student student = /*em.merge(*/student0/*)*/;
        //Student student = em.merge(student0);
        List<Module> studentModules = new ArrayList<>(student.getModules());
        String semester = studentModules.get(0).getSubject().getSemester();
        //logger.debug("saving student {} modules: {}", student.name, studentModules.size());

        try (Connection connection = sql2o.beginTransaction(ThreadLocalTransaction.get())) {

            SubjectsCache subjectsCache = new SubjectsCache(connection);

            List<Module> currentModules = connection
                    .createQuery("select *, modules.id as id from modules JOIN subjects ON modules.subject_id = subjects.id WHERE student_id = :id AND semester = :semester")
                    .addParameter("id", student.id)
                    .addParameter("semester", semester)
                    .throwOnMappingFailure(false)
                    .executeAndFetch(Module.class).stream()
                    .map(subjectsCache::loadSubject)
                    .collect(Collectors.toList());

            for (Module module : studentModules) {
                Optional<Module> existigModule = currentModules.stream().filter(cur ->
                        cur.getSubject().equals(module.getSubject()) && cur.getNum().equals(module.getNum())).findAny();

                if(existigModule.isPresent()){
                    connection
                            .createQuery("UPDATE modules SET color = :color, value = :value WHERE id = :id")
                            .addParameter("color", module.getColor())
                            .addParameter("value", module.getValue())
                            .addParameter("id", existigModule.get().getId())
                            .executeUpdate();

                    currentModules.remove(existigModule.get());
                } else {
                    connection
                            .createQuery("INSERT INTO modules (color, num, value, student_id, subject_id) VALUES (" +
                                    ":color, :num, :value, :studentId, :subjectId)")
                            .bind(module)
                            .addParameter("studentId", student.id)
                            .addParameter("subjectId", module.getSubject().getId())
                            .executeUpdate();
                }


            }

            if (!currentModules.isEmpty()) {
                logger.debug("removing modules {}", currentModules);
                connection
                        .createQuery("DELETE FROM modules WHERE id IN (:ids)")
                        .addParameter("ids", currentModules.stream().map(Module::getId).collect(Collectors.toList()))
                        .executeUpdate();
            }

            connection.commit();
        }

    }

    @Override
    public void deleteStudentModules(Student student, String semester) {

        try (Connection connection = sql2o.beginTransaction().setRollbackOnClose(false)) {

            connection
                    .createQuery("DELETE FROM modules WHERE student.id = :studentid && subject_id in (SELECT subjects.id FROM subjects WHERE semester = :semester)")
                    .addParameter("studentid", student.id)
                    .addParameter("semester", semester)
                    .executeUpdate();
        }

    }

    @Override
    public void deleteAllModules(String semester) {
        try (Connection connection = sql2o.beginTransaction().setRollbackOnClose(false)) {

            connection
                    .createQuery("DELETE FROM modules WHERE subject_id in (SELECT subjects.id FROM subjects WHERE semester = :semester)")
                    .addParameter("semester", semester)
                    .executeUpdate();
            connection.commit();
        }
    }

    @Override
    public void deleteStudent(Student s) {

        logger.debug("deleting student {}", s);

        try (Connection connection = sql2o.beginTransaction(ThreadLocalTransaction.get())) {
            connection.createQuery("DELETE FROM users WHERE id=:id;").addParameter("id", s.id).executeUpdate();
            connection.commit();
        }

    }

    @Override
    public void saveStudent(Student student, String semestr) {

        try (Connection connection = sql2o.beginTransaction(ThreadLocalTransaction.get())) {

            logger.trace("saving student {} at semester {}", student, semestr);

            if(student.id == 0){
                logger.trace("inserting student {}", student);
                Integer userId = connection
                        .createQuery("INSERT INTO users (login, initials, name, patronym, surname) " +
                                "VALUES (:cardid, :initials, :name, :patronym, :surname)", true)
                        .bind(student)
                        .executeUpdate().getKey(Integer.class);

                student.id = userId;

                connection.createQuery("INSERT INTO student (id, stgroup) " +
                        "VALUES (:id, :stgroup)")
                        .bind(student)
                        .executeUpdate();
            } else {
                logger.trace("updating student {}", student);
                connection
                        .createQuery("UPDATE users  SET login = :cardid," +
                                " initials = :initials," +
                                " name = :name," +
                                " patronym = :patronym," +
                                " surname = :surname" +
                                " WHERE id = :id")
                        .bind(student)
                        .executeUpdate();

                connection.createQuery("UPDATE student SET stgroup = :stgroup " +
                        "WHERE id = :id")
                        .bind(student)
                        .executeUpdate();
            }


            logger.trace("updating student groups history {} at semester {}", student, semestr);

            StudentHistoricalGroup group = connection
                    .createQuery("SELECT * from groupshistory WHERE student_id = :studentId and semestr = :semester LIMIT 1")
                    .addParameter("studentId", student.id)
                    .addParameter("semester", semestr)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(StudentHistoricalGroup.class);

            logger.trace("updating student group at semester {} {}", semestr, group);

            if (group == null) {
                connection
                        .createQuery("INSERT INTO groupshistory (groupname, semestr, student_id) " +
                                "VALUES (:group, :semester, :student)")
                        .addParameter("group", student.stgroup)
                        .addParameter("semester", semestr)
                        .addParameter("student", student.id)
                        .executeUpdate();
            } else if (!group.groupName.equals(student.stgroup)) {
                connection
                        .createQuery("UPDATE groupshistory SET groupname = :group WHERE id = :entryid")
                        .addParameter("group", student.stgroup)
                        .addParameter("entryid", group.id)
                        .executeUpdate();
            }

            connection.commit();
        }
    }


    @Override
    public Stream<Student> getStudents() {
        return getStudentsFiltred("");
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        logger.debug("getStudentsFiltred '{}'", text);
        if (text == null)
            return getStudents();

        Connection connection = sql2o.open(ThreadLocalTransaction.get());
        try {
            return toStream(connection
                    .createQuery("SELECT users.id AS id, users.login AS cardid, * FROM users INNER JOIN student ON users.id = student.id  WHERE surname || initials || stgroup || users.login ILIKE :pattern ORDER BY stgroup, surname;\n")
                    .addParameter("pattern", "%" + text + "%")
                    .throwOnMappingFailure(false)
                    .executeAndFetchLazy(Student.class), connection);

        } catch (Exception e) {
            connection.close();
            throw e;
        }

    }


    @Override
    public Student getStudentById(int id, String semester) {

        try (Connection connection = sql2o.open()) {
            Student student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" +
                            " WHERE users.id = :id")
                    .addParameter("id", id)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student.class);

            if(student != null) {
                student.setGroups(getStudentHistoricalGroups(connection, id));
                if (semester != null) {
                    student.setModules(getStudentModules(connection, semester, id));
                }
            }

            return student;
        }
    }

    private List<Module> getStudentModules(Connection connection, String semester, int studentid) {
        SubjectsCache subjectsCache = new SubjectsCache(connection);
        return connection
                .createQuery("SELECT * from modules WHERE student_id = :id and subject_id in (SELECT subjects.id from subjects WHERE semester = :semester)")
                .addParameter("id", studentid)
                .addParameter("semester", semester)
                .throwOnMappingFailure(false)
                .executeAndFetch(Module.class).stream()
                .map(subjectsCache::loadSubject).collect(Collectors.toList());
    }

    private List<StudentHistoricalGroup> getStudentHistoricalGroups(Connection connection, int studentid) {
        return connection
                .createQuery("SELECT * FROM groupshistory WHERE student_id = :id")
                .addParameter("id", studentid)
                .throwOnMappingFailure(false)
                .executeAndFetch(StudentHistoricalGroup.class);
    }

    @Override
    public Set<String> getStudentSemestersWithMarks(int student) {
        try (Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT DISTINCT semester FROM subjects WHERE subjects.id in (SELECT DISTINCT subject_id FROM modules WHERE student_id = :studentId)")
                    .addParameter("studentId", student)
                    .executeScalarList(String.class)
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet<String>::new));
        }
    }

    @Override
    public Set<String> getKnownSemesters() {
        try (Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT DISTINCT semester FROM subjects WHERE subjects.id in (SELECT DISTINCT subject_id FROM modules)")
                    .executeScalarList(String.class)
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet<String>::new));
        }
    }

    @Override
    public Subject getOrCreateSubject(String semester, String group, String name, double factor) {

        try (Connection connection = sql2o.beginTransaction()) {
            Subject subject = connection.createQuery("SELECT * FROM subjects WHERE semester = :semester AND stgroup = :group AND subjects.title = :title LIMIT 1")
                    .addParameter("semester", semester)
                    .addParameter("group", group)
                    .addParameter("title", name)
                    .executeAndFetchFirst(Subject.class);
            if(subject != null && Math.abs(subject.getFactor() - factor) < 0.001){
                return subject;
            } else if(subject == null){
                int id = connection
                        .createQuery("INSERT INTO subjects (factor, stgroup, title, semester) VALUES (:factor, :group, :title, :semester)")
                        .addParameter("semester", semester)
                        .addParameter("group", group)
                        .addParameter("title", name)
                        .addParameter("factor", factor)
                        .executeUpdate().getKey(Integer.class);
                connection.commit();
                return new Subject(id, semester, group, name, factor);
            } else {
                connection
                        .createQuery("UPDATE subjects SET factor = :factor WHERE id = :id")
                        .addParameter("factor", factor)
                        .addParameter("id", subject.getId())
                        .executeUpdate();
                subject.setFactor(factor);
                connection.commit();
                return subject;
            }
        }
    }

    @Override
    public Student getStudentByGroupSurnameInitials(String semester, String group, String surname, String initials) {

        try (Connection connection = sql2o.open()) {
            Student student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" +
                            " WHERE student.stgroup = :group and users.surname = :surname AND users.initials = :initials")
                    .addParameter("group", group)
                    .addParameter("surname", surname)
                    .addParameter("initials", initials)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student.class);

            if (student != null) {
                student.setGroups(getStudentHistoricalGroups(connection, student.id));
                if (semester != null) {
                    student.setModules(getStudentModules(connection, semester, student.id));
                }
            }

            return student;
        }
    }


    @Override
    public Student getStudentByCardId(String cardid) {

        try (Connection connection = sql2o.open(ThreadLocalTransaction.get())) {
            Student student = connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" +
                            " WHERE login = :login")
                    .addParameter("login", cardid)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student.class);

            if (student != null)
                student.setGroups(getStudentHistoricalGroups(connection, student.id));

            return student;
        }
    }

    private <T> Stream<T> toStream(ResultSetIterable<T> resultSet, Connection connection) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resultSet.iterator(),
                Spliterator.DISTINCT | Spliterator.NONNULL |
                        Spliterator.CONCURRENT | Spliterator.IMMUTABLE
        ), false).onClose(() -> {
            resultSet.close();
            connection.close();
        });
    }

}
