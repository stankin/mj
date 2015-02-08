package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
    public void updateModules(Student student) {
        logger.debug("saving student {}", student);
        student.getModules().forEach(m -> m.student = student);
        em.persist(student);
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