package ru.stankin.mj.model;

import java.io.Serializable;

/**
 * Created by nickl on 08.01.15.
 */
public class Module implements Cloneable, Serializable {

    public String subject = "";
    public int num = 0;
    public int value = -1;

    @Override
    public Module clone()  {
        try {
            return (Module) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Module(String subject, int num) {
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
