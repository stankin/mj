package ru.stankin.mj.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by nickl on 01.02.15.
 */

public interface Storage {
    void updateModules(Student student);

    Stream<Student> getStudents();

    Stream<Student> getStudentsFiltred(String text);

    Student getStudentById(int id, String semester);

    Set<String> getStudentSemesters(int student);

    Set<String> getKnownSemesters();

    Student getStudentByCardId(String cardid);

    void deleteStudentModules(Student student, String semester);

    void saveStudent(Student student);

    Subject getOrCreateSubject(String semester, String group, String name, double factor);

    Student getStudentByGroupSurnameInitials(String group, String surname, String initials);

    void deleteAllModules(String semestr);

    void deleteStudent(Student s);
}
