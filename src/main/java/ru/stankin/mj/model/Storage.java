package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Created by nickl on 08.01.15.
 */

@ApplicationScoped
public class Storage {

    private static final Logger logger = LogManager.getLogger(Storage.class);

    private TreeSet<Student> students = new TreeSet<>();
    private ArrayList<Student> studentsList = new ArrayList<>();

    public synchronized void updateModules(Student student) {
        //System.out.println("updateModules:"+student);
        if (students.add(student)) {
            student.id = students.size()-1;
            studentsList.add(student);
        } else {
            Student floor = students.floor(student);
            //logger.debug("floor: {}", floor);
            floor.modules = student.modules;
        }

        //logger.debug("studentscount: {}",students.size());
    }

    public Collection<Student> getStudents() {
        return students;
    }

    public Stream<Student> getStudentsFiltred(String text) {
        //logger.debug("students: {}",students);
        return students.stream().filter(s ->
                s.group.contains(text) ||
                s.surname.contains(text) ||
                s.initials.contains(text)
        );
    }

    public Student getStudentById(int value) {
        return studentsList.get(value);
    }
}
