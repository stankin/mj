package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;


public class Subject implements Serializable {

    private static final Logger logger = LogManager.getLogger(Subject.class);


    private int id = 0;

    private String semester = "2014/2015 весна";
    private String stgroup = "";
    private String title = "";
    private double factor = 0;

    public Subject(int id, String semester, String stgroup, String title, double factor) {
        this.id = id;
        this.semester = semester;
        this.stgroup = stgroup;
        this.title = title;
        this.factor = factor;
    }

    public Subject() {
    }

    public int getId() {
        return id;
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
                ", semester='" + semester + '\'' +
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

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}
