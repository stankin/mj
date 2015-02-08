package ru.stankin.mj.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "Modules")
public class Module implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private int id = 0;

    private String subject = "";
    private String num = "";
    private int value = -1;
    private int color = 0;

    @ManyToOne
    @JoinColumn(name = "student_id")
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

    public Module(String subject, String num) {
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
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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
}
