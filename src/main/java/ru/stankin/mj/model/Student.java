package ru.stankin.mj.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by nickl on 08.01.15.
 */
public class Student implements Serializable, Comparable {

    public int id = 0;

    public String group;
    public String surname;
    public String initials;

    public String login = "";
    public String password = "";

    public ArrayList<Module> modules = new ArrayList<>();

    public Student() {
    }

    public Student(String group, String surname, String initials) {
        this.group = group;
        this.surname = surname;
        this.initials = initials;
    }

    @Override
    public String toString() {
        return "Student{" +
                "'" + group + '\'' +
                ", '" + surname + '\'' +
                ", '" + initials + '\'' +
                ", " + modules +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        Student a = (Student) o;
        int gc = group.compareTo(a.group);
        return gc != 0 ? gc :
                surname.compareTo(a.surname) != 0 ? surname.compareTo(a.surname) :
                        initials.compareTo(a.initials);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Student student = (Student) o;

        if (group != null ? !group.equals(student.group) : student.group != null) return false;
        if (initials != null ? !initials.equals(student.initials) : student.initials != null) return false;
        if (surname != null ? !surname.equals(student.surname) : student.surname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + (initials != null ? initials.hashCode() : 0);
        return result;
    }
}
