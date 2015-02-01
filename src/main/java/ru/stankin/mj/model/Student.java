package ru.stankin.mj.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickl on 08.01.15.
 */
@Entity
@Table(name = "Student")
public class Student implements Serializable, Comparable {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public int id = 0;

    public String stgroup;
    public String surname;
    public String initials;

    public String login = "";
    public String password = "";

    @ElementCollection
    //@Transient
    private List<Module> modules = new ArrayList<>();

    public Student() {
    }

    public Student(String group, String surname, String initials) {
        this.stgroup = group;
        this.surname = surname;
        this.initials = initials;
    }



    @Override
    public String toString() {
        return "Student{" +
                "'" + stgroup + '\'' +
                ", '" + surname + '\'' +
                ", '" + initials + '\'' +
                ", " + getModules() +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        Student a = (Student) o;
        int gc = stgroup.compareTo(a.stgroup);
        return gc != 0 ? gc :
                surname.compareTo(a.surname) != 0 ? surname.compareTo(a.surname) :
                        initials.compareTo(a.initials);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Student student = (Student) o;

        if (stgroup != null ? !stgroup.equals(student.stgroup) : student.stgroup != null) return false;
        if (initials != null ? !initials.equals(student.initials) : student.initials != null) return false;
        if (surname != null ? !surname.equals(student.surname) : student.surname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stgroup != null ? stgroup.hashCode() : 0;
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + (initials != null ? initials.hashCode() : 0);
        return result;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }
}
