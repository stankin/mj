package ru.stankin.mj.model;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "Subjects", indexes = {@Index(name = "title_index", columnList = "title")})
public class Subject implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id = 0;

    private String title = "";
    private double factor = 0;

    public Subject(String name) {
        this.title = name;
    }

    public Subject() {
    }


    public double getFactor() {
        return factor;
    }

    public void setFactor(double factor) {
        this.factor = factor;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    //public static final Subject RAITING = new Subject("Рейтинг");
    //public static final Subject ACCUMULATED_RAITING = new Subject("Накопленный Рейтинг");
}
