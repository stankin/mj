package ru.stankin.mj.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "Modules")
public class Module implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public int id = 0;

    public String subject = "";
    public String num = "";
    public int value = -1;
    public int color = 0;

    @ManyToOne
    @JoinColumn(name = "student_id")
    Student student;

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
        this.subject = subject;
        this.num = num;
    }

    public Module() {
    }

    @Override
    public String toString() {
        return "Module{" +
                "'" + subject + '\'' +
                ", " + num +
                ", " + value +
                '}';
    }
}
