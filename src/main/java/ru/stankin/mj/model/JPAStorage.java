package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hibernate.criterion.Restrictions.ilike;

/**
 * Created by nickl on 01.02.15.
 */
@ApplicationScoped
@Default
public class JPAStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(ModuleJournalUploader.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    @javax.transaction.Transactional
    public void updateModules(Student student0) {

        Student student = /*em.merge(*/student0/*)*/;
        //Student student = em.merge(student0);
        List<Module> studentModules = new ArrayList<>(student.getModules());
        logger.debug("saving student {} modules: {}", student.name, studentModules.size());

        //TODO: но вообще это какой-то ад

        deleteStudentModules(student);

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
    @javax.transaction.Transactional
    public void deleteStudentModules(Student student) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaDelete<Module> query = b.createCriteriaDelete(Module.class);
        Root<Module> from = query.from(Module.class);
        query.where(b.equal(from.get("student"), student));
        int deleted = em.createQuery(query).executeUpdate();
        logger.debug("deleted {}", deleted);
        em.flush();
    }

    @Override
    @javax.transaction.Transactional
    public void saveStudent(Student student) {
        Student merge = em.merge(student);
        if(merge.password == null) {
            merge.password = merge.cardid;
            em.merge(merge);
        }
    }



    @Override
    public Stream<Student> getStudents() {
        return getStudentsFiltred("");
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        logger.debug("getStudentsFiltred {}", text);
        if (text == null)
            return getStudents();

        Session session = (Session) em.getDelegate();
        Criteria criteria = session.createCriteria(Student.class);
        criteria
                .addOrder(Order.asc("stgroup"))
                .addOrder(Order.asc("surname"));
        criteria.add(Restrictions.or(
                ilike("stgroup", text, MatchMode.ANYWHERE),
                ilike("surname", text, MatchMode.ANYWHERE),
                ilike("initials", text, MatchMode.ANYWHERE)
        ));
        criteria.setFetchSize(Integer.valueOf(1000));
        criteria.setReadOnly(true);
        criteria.setCacheable(false);
        criteria.setLockMode("a", LockMode.NONE);

        ScrollableResults scroll = criteria.scroll(ScrollMode.FORWARD_ONLY);

        return toStream(scroll, Student.class);

    }

    @Override
    @javax.transaction.Transactional
    public Student getStudentById(int value, boolean eager) {
        Student student = em.find(Student.class, value);
        if (eager) {
            student.getModules().size();
//            ArrayList<Module> modules = new ArrayList<>(student.getModules());
//            //logger.debug("modules:{}", modules);
//            student.setModules(modules);
        }
        return student;
    }

    @Override
    @javax.transaction.Transactional
    public Student getStudentByGroupSurnameInitials(String group, String surname, String initials) {
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
        }
        catch (NoResultException e){
            return null;
        }
    }

    @Override
    public Student getStudentByCardId(String cardid) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<Student> query = b.createQuery(Student.class);
        Root<Student> from = query.from(Student.class);
        query.where(b.equal(from.get("cardid"), cardid));

        try {
            TypedQuery<Student> query1 = em.createQuery(query);
            query1.setFlushMode(FlushModeType.COMMIT);
            query1.setMaxResults(1);
            return query1.getSingleResult();
        }
        catch (javax.persistence.NoResultException e){
            return null;
        }
    }



    private <T> Spliterator<T> toSplitIterator(ScrollableResults scroll, Class<T> type){
        return Spliterators.spliteratorUnknownSize(
                new ScrollableResultIterator<>(scroll, type),
                Spliterator.DISTINCT | Spliterator.NONNULL |
                        Spliterator.CONCURRENT | Spliterator.IMMUTABLE
        );
    }

    private <T> Stream<T> toStream(ScrollableResults scroll, Class<T> type){
       return StreamSupport.stream(toSplitIterator(scroll, type), false);
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