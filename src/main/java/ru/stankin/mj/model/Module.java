package ru.stankin.mj.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Module implements Cloneable, Serializable {

    public static final int BLACK_MODULE = 3355443;

    private static final long serialVersionUID = 1L;


    private int id = 0;


    private Subject subject = null;

    private String num = "";
    private int value = -1;
    private int color = 0;

    @JsonIgnore
    private Student student;

    @Override
    public Module clone()  {
        try {
            return (Module) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Module(Subject subject, String num) {
        this.setSubject(subject);
        this.setNum(num);
    }

    public Module() {
    }

    @Override
    public String toString() {
        return "Module{" +
                "'" + getSubject() + '\'' +
                ", " + getNum() +
                ", " + getValue() +
                ", color=" + getColor() +
                '}';
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        return subject;
    }

    public boolean disabled() {
        return getColor() == BLACK_MODULE;
    }
}
