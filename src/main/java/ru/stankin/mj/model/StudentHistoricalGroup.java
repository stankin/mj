package ru.stankin.mj.model;


import java.io.Serializable;

public class StudentHistoricalGroup implements Serializable {

    public StudentHistoricalGroup() {
    }

    public StudentHistoricalGroup(Student student, String semestr, String groupName) {
        this.groupName = groupName;
        this.semestr = semestr;
        this.student = student;
    }

    private static final long serialVersionUID = 1L;

    public int id = 0;

    public Student student;

    public String semestr;

    public String groupName;

}
