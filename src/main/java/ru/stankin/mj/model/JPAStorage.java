package ru.stankin.mj.model;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by nickl on 01.02.15.
 */
@ApplicationScoped
@Default
public class JPAStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(ModuleJournal.class);

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
        return (Collection<Student>)em.createQuery("Select s from Student s").getResultList();
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        return null;
    }

    @Override
    @javax.transaction.Transactional
    public Student getStudentById(int value, boolean eager) {
        Student student = em.find(Student.class, value);
        if(eager) {
            student.getModules().size();
//            ArrayList<Module> modules = new ArrayList<>(student.getModules());
//            //logger.debug("modules:{}", modules);
//            student.setModules(modules);
        }
        return student;
    }
}
