package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

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
        em.persist(student);
    }

    @Override
    public Collection<Student> getStudents() {
        return (Collection<Student>) em.createQuery("Select s from Student s").getResultList();
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        logger.debug("getStudentsFiltred {}", text);
        if (text == null)
            return getStudents().stream();

        Session session = (Session) em.getDelegate();
        Criteria criteria = session.createCriteria(Student.class);
        criteria.add(Restrictions.or(
                ilike("stgroup", text, MatchMode.ANYWHERE),
                ilike("surname", text, MatchMode.ANYWHERE),
                ilike("initials", text, MatchMode.ANYWHERE)
                ));

        List resultList = criteria.list();
        return ((Collection<Student>) resultList).stream();

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
}
