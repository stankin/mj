package ru.stankin.mj.model;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Created by nickl on 08.01.15.
 */

@ApplicationScoped
public class Storage {

    private TreeSet<Student> students = new TreeSet<>();
    private ArrayList<Student> studentsList = new ArrayList<>();

    public synchronized void updateModules(Student student) {
        System.out.println("updateModules:"+student);
        if (students.add(student)) {
            student.id = students.size()-1;
            studentsList.add(student);
        } else {
            Student floor = students.floor(student);
            System.out.println("floor:"+floor);
            floor.modules = student.modules;
        }

        System.out.println("studentscount:"+students.size());
    }

    public Iterable<Student> getStudents() {
        return students;
    }

    public Stream<Student> getStudentsFiltred(String text) {
        System.out.println("students:"+students);
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
