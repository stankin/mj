package ru.stankin.mj.model;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by nickl on 01.02.15.
 */

public interface Storage {
    void updateModules(Student student);

    Stream<Student> getStudents();

    Stream<Student> getStudentsFiltred(String text);

    Student getStudentById(int value, boolean eager);

    Student getStudentByCardId(String cardid);

    void deleteStudentModules(Student student);

    void saveStudent(Student student);

    @javax.transaction.Transactional
    Subject getSubjectByName(String name);

    Student getStudentByGroupSurnameInitials(String group, String surname, String initials);

    void deleteAllModules();

    void deleteStudent(Student s);
}
