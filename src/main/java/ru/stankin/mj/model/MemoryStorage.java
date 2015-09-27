package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    public Stream<Student> getStudents() {
        return students.stream();
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
    public Student getStudentById(int id, String semester) {
        return studentsList.get(id);
    }

    @Override
    public Student getStudentByCardId(String cardid) {
        return null;
    }

    @Override
    public void deleteStudentModules(Student student) {
        throw new UnsupportedOperationException("deleteStudentModules");
    }

    @Override
    public void saveStudent(Student student) {
        students.add(student);
    }

    private Map<String, Subject> stringSubjectMap = new HashMap<>();

    @Override
    public Subject getOrCreateSubject(String semester, String group, String name, double factor) {
        Subject subject = stringSubjectMap.get(name);
        if(subject == null){
            subject = new Subject(semester, group, name, factor);
            stringSubjectMap.put(name, subject);
        }
        return subject;
    }

    @Override
    public Student getStudentByGroupSurnameInitials(String group, String surname, String initials) {
        return new Student(group, surname, initials);
    }

    @Override
    public void deleteAllModules() {
        throw new UnsupportedOperationException("deleteAllModules");
    }

    @Override
    public void deleteStudent(Student s) {
        throw new UnsupportedOperationException("deleteStudent");
    }
}
