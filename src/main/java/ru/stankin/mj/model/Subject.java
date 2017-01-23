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

    public Subject(String semester, String stgroup, String title, double factor) {
        this(0, semester, stgroup, title, factor);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject)) return false;

        Subject subject = (Subject) o;

        if (id != subject.id) return false;
        if (Double.compare(subject.factor, factor) != 0) return false;
        if (semester != null ? !semester.equals(subject.semester) : subject.semester != null) return false;
        if (stgroup != null ? !stgroup.equals(subject.stgroup) : subject.stgroup != null) return false;
        return title != null ? title.equals(subject.title) : subject.title == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id;
        result = 31 * result + (semester != null ? semester.hashCode() : 0);
        result = 31 * result + (stgroup != null ? stgroup.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        temp = Double.doubleToLongBits(factor);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
