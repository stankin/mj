package ru.stankin.mj.model.user;

/**
 * Created by nickl on 16.02.15.
 */
public interface User {
    String getUsername();

    String getEmail();

    int getId();

    boolean isAdmin();

}
