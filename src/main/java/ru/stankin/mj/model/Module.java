package ru.stankin.mj.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Module implements Cloneable, Serializable {

    public static final int BLACK_MODULE = 3355443;

    private static final long serialVersionUID = 1L;


    private Subject subject = null;

    private String num = "";
    private int value = -1;
    private int color = 0;

    @JsonIgnore
    public int studentId;

    @JsonIgnore
    public int subjectId;

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
        this.subjectId = subject.getId();
    }

    public Module(Subject subject, int studentId, String num) {
       this(subject, num);
       this.studentId = studentId;
    }

    public Module(Subject subject, int studentId, String num, int value, int color) {
        this(subject, studentId, num);
        this.value = value;
        this.color = color;
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

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        return subject;
    }

    public boolean disabled() {
        return getColor() == BLACK_MODULE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module)) return false;

        Module module = (Module) o;

        if (value != module.value) return false;
        if (color != module.color) return false;
        if (studentId != module.studentId) return false;
        if (subject.getId() != module.subject.getId()) return false;
        return num.equals(module.num);
    }

    @Override
    public int hashCode() {
        int result = subject.hashCode();
        result = 31 * result + num.hashCode();
        result = 31 * result + value;
        result = 31 * result + color;
        result = 31 * result + studentId;
        return result;
    }
}
