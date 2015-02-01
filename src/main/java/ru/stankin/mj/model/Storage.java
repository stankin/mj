package ru.stankin.mj.model;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by nickl on 01.02.15.
 */

public interface Storage {
    void updateModules(Student student);

    Collection<Student> getStudents();

    Stream<Student> getStudentsFiltred(String text);

    Student getStudentById(int value, boolean eager);
}
