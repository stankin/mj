package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Created by nickl on 08.01.15.
 */

@Alternative
public class MemoryStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(MemoryStorage.class);

    private TreeSet<Student> students = new TreeSet<>();
    private ArrayList<Student> studentsList = new ArrayList<>();

    @Override
    public synchronized void updateModules(Student student) {
        //System.out.println("updateModules:"+student);
        if (students.add(student)) {
            student.id = students.size()-1;
            studentsList.add(student);
        } else {
            Student floor = students.floor(student);
            //logger.debug("floor: {}", floor);
            floor.setModules(student.getModules());
        }

        //logger.debug("studentscount: {}",students.size());
    }

    @Override
    public Collection<Student> getStudents() {
        return students;
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        //logger.debug("students: {}",students);
        return students.stream().filter(s ->
                s.stgroup.contains(text) ||
                s.surname.contains(text) ||
                s.initials.contains(text)
        );
    }

    @Override
    public Student getStudentById(int value, boolean eager) {
        return studentsList.get(value);
    }
}