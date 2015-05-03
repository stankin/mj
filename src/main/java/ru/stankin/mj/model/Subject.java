package ru.stankin.mj.model;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "Subjects", indexes = {@Index(name = "title_index", columnList = "stgroup,title")})
public class Subject implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id = 0;

    private String stgroup = "";
    private String title = "";
    private double factor = 0;

    public Subject(String stgroup, String title, double factor) {
        this.stgroup = stgroup;
        this.title = title;
        this.factor = factor;
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

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", group='" + stgroup + '\'' +
                ", title='" + title + '\'' +
                ", factor=" + factor +
                '}';
    }

    public String getStgroup() {
        return stgroup;
    }

    public void setStgroup(String group) {
        this.stgroup = group;
    }
    
}
