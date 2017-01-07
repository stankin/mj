package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.criterion.Order;
import org.sql2o.Connection;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;
import org.sql2o.StatementRunnableWithResult;

import javax.ejb.*;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import javax.persistence.criteria.CriteriaQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hibernate.criterion.Restrictions.ilike;

/**
 * Created by nickl on 01.02.15.
 */
@javax.ejb.Singleton
@javax.ejb.Startup
//@ApplicationScoped
//@javax.inject.Singleton
@Default
//@Lock(LockType.READ)
public class JPAStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(JPAStorage.class);

    private final
    Sql2o sql2o;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public JPAStorage(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @Override
    //@javax.transaction.Transactional
    public void updateModules(Student student0) {

        Student student = /*em.merge(*/student0/*)*/;
        //Student student = em.merge(student0);
        List<Module> studentModules = new ArrayList<>(student.getModules());
        String semester = studentModules.get(0).getSubject().getSemester();
        //logger.debug("saving student {} modules: {}", student.name, studentModules.size());

        //TODO: но вообще это какой-то ад

        deleteStudentModules(student, semester);

        studentModules.forEach(m -> {
            m.setStudent(student);
            em.persist(m);
            //logger.debug("persist module {} ",   m);
        });
        //em.flush();
        //em.refresh(student);
//        student.getModules().forEach(m -> {
//            m.setStudent(student);
//            em.refresh(m);
//            logger.debug("refresing module {} {}", m.getStudent(), m);
//        });
    }

    @Override
    //@javax.transaction.Transactional
    public void deleteStudentModules(Student student, String semester) {

        Query query = em.createQuery(
                "DELETE from Module m where m in (select sm from Module sm where sm.student = :student and sm.subject.semester = :semester)"
        );
        query.setParameter("student", student);
        query.setParameter("semester", semester);
        query.executeUpdate();

        em.flush();
    }

    @Override
//    //@javax.transaction.Transactional
    public void deleteAllModules(String semester) {
        Query query = em.createQuery(
                "DELETE from Module m where m in (select sm from Module sm where sm.subject.semester = :semester)"
        );
        query.setParameter("semester", semester);
        query.executeUpdate();
//        CriteriaBuilder b = em.getCriteriaBuilder();
//        CriteriaDelete<Module> query = b.createCriteriaDelete(Module.class);
//        Root<Module> from = query.from(Module.class);
//        //query.where(b.equal(from.get("student"), student));
//        int deleted = em.createQuery(query).executeUpdate();
//        //logger.debug("deleted {}", deleted);
        em.flush();
    }

    @Override
    public void deleteStudent(Student s) {

        logger.debug("deleting student {}", s);

        try (Connection connection = sql2o.beginTransaction()) {
            connection.createQuery("DELETE FROM users WHERE id=:id;").addParameter("id", s.id).executeUpdate();
        }

    }

    @Override
    public void saveStudent(Student student, String semestr) {

        try (Connection connection = sql2o.beginTransaction()) { //TODO: на самом деле должны быть одна транзакция на всех студентов

            if(student.password == null){
                student.password = student.cardid;
            }

            logger.trace("saving student {} at semester {}", student, semestr);

            if(student.id == 0){
                logger.trace("inserting student {}", student);
                Integer userId = connection
                        .createQuery("INSERT INTO users (login, initials, name, patronym, surname) " +
                                "VALUES (:cardid, :initials, :name, :patronym, :surname)", true)
                        .bind(student)
                        .executeUpdate().getKey(Integer.class);

                student.id = userId;

                connection.createQuery("INSERT INTO student (id, password, stgroup) " +
                        "VALUES (:id, :password, :stgroup)")
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

                connection.createQuery("UPDATE student SET password = :password, stgroup = :stgroup " +
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
    ////@javax.transaction.Transactional
    public Stream<Student> getStudents() {
        return getStudentsFiltred("");
    }

    @Override
    //@TransactionAttribute(value = TransactionAttributeType.SUPPORTS)
    public Stream<Student> getStudentsFiltred(String text) {
        logger.debug("getStudentsFiltred '{}'", text);
        if (text == null)
            return getStudents();

        Connection connection = sql2o.open();
        try {
            return toStream(connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id  WHERE surname || initials || stgroup ILIKE :pattern ORDER BY stgroup, surname;\n")
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

            if (semester != null) {
                Map<Integer, Subject> subjectsCache = new HashMap<>();
                student.setModules(connection
                        .createQuery("SELECT * from modules WHERE student_id = :id and subject_id in (SELECT subjects.id from subjects WHERE semester = :semester)")
                        .addParameter("id", id)
                        .addParameter("semester", semester)
                        .throwOnMappingFailure(false)
                        .executeAndFetch(Module.class).stream()
                        .map(module -> {
                            Subject subject = connection
                                    .createQuery("SELECT * from subjects WHERE subjects.id in (SELECT subject_id FROM modules WHERE modules.id = :id)")
                                    .addParameter("id", module.getId())
                                    .throwOnMappingFailure(false).executeAndFetchFirst(Subject.class);

                            //TODO: вообще их можно было бы кешировать глобально и отдельно, всеравно предметы у модулей не меняются
                            Subject cashedSubject = subjectsCache.computeIfAbsent(subject.getId(), i -> subject);
                            module.setSubject(cashedSubject);
                            return module;
                        }).collect(Collectors.toList())
                );
            }

            return student;
        }
    }

    @Override
    public Set<String> getStudentSemestersWithMarks(int student) {
        TypedQuery<String> query = em.createQuery(
                "select distinct sm.subject.semester from Module sm where sm.student.id = :student",
                String.class
        );
        query.setParameter("student", student);
        return query.getResultList().stream().filter(e -> e != null).collect(Collectors.toCollection(TreeSet<String>::new));
    }

    @Override
    public Set<String> getKnownSemesters() {
        TypedQuery<String> query = em.createQuery(
                "select distinct sm.subject.semester from Module sm",
                String.class
        );
        return query.getResultList().stream().filter(e -> null != e).collect(Collectors.toCollection(TreeSet<String>::new));
    }

    @Override
    //@javax.transaction.Transactional
    public Subject getOrCreateSubject(String semester, String group, String name, double factor) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Subject> query = b.createQuery(Subject.class);
        Root<Subject> from = query.from(Subject.class);
        query.where(b.and(
                b.equal(from.get("semester"), semester),
                b.equal(from.get("stgroup"), group),
                b.equal(from.get("title"), name)
        ));
        try {
            TypedQuery<Subject> query1 = em.createQuery(query);
            query1.setFlushMode(FlushModeType.COMMIT);
            Subject storedSubject = query1.getSingleResult();
            if (storedSubject.getFactor() != factor) {
                storedSubject.setFactor(factor);
                em.merge(storedSubject);
                em.flush();
            }
            //storedSubject.getModules().size();
            return storedSubject;
        } catch (NoResultException e) {
            //TODO: при таком подходе нужна очищалка неиспользуемых предметов
            Subject subject = em.merge(new Subject(semester, group, name, factor));
            em.flush();
            return subject;
        }
    }

    @Override
    //@javax.transaction.Transactional
    public Student getStudentByGroupSurnameInitials(String semestr, String group, String surname, String initials) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Student> query = b.createQuery(Student.class);
        Root<Student> from = query.from(Student.class);
        query.where(b.and(
                b.equal(from.get("stgroup"), group),
                b.equal(from.get("surname"), surname),
                b.equal(from.get("initials"), initials)
        ));
        try {
            Student singleResult = em.createQuery(query).getSingleResult();
            //singleResult.getModules().size();
            return singleResult;
        } catch (NoResultException e) {

            try {
                TypedQuery<Student> query2 = em.createQuery("Select s from Student s where s.surname = :surname and s.initials = :initials" +
                        " and  :stgroup in (Select g.groupName from s.groups g where g.semestr = :semestr)", Student.class);
                query2.setParameter("surname", surname);
                query2.setParameter("initials", initials);
                query2.setParameter("stgroup", group);
                query2.setParameter("semestr", semestr);

                return query2.getSingleResult();
            } catch (NoResultException e2) {
                return null;
            }
        }
    }


    @Override
    public Student getStudentByCardId(String cardid) {

        try (Connection connection = sql2o.open()) {
            return connection
                    .createQuery("SELECT users.id as id, users.login as cardid, * FROM users INNER JOIN student on users.id = student.id" +
                            " WHERE login = :login")
                    .addParameter("login", cardid)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(Student.class);
        }
    }


    private <T> Spliterator<T> toSplitIterator(ScrollableResults scroll, Class<T> type) {
        return Spliterators.spliteratorUnknownSize(
                new ScrollableResultIterator<>(scroll, type),
                Spliterator.DISTINCT | Spliterator.NONNULL |
                        Spliterator.CONCURRENT | Spliterator.IMMUTABLE
        );
    }

    private <T> Stream<T> toStream(ScrollableResults scroll, Class<T> type) {
        return StreamSupport.stream(toSplitIterator(scroll, type), false);
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

class ScrollableResultIterator<T> implements Iterator<T> {
    private final ScrollableResults results;
    private final Class<T> type;

    ScrollableResultIterator(ScrollableResults results, Class<T> type) {
        this.results = results;
        this.type = type;
    }

    @Override
    public boolean hasNext() {
        return results.next();
    }

    @Override
    public T next() {
        return type.cast(results.get(0));
    }
}