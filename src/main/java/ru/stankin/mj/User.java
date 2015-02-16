package ru.stankin.mj;

/**
 * Created by nickl on 16.02.15.
 */
public interface User {
    String getUsername();

    String getPassword();

    void setPassword(String password);

    boolean isAdmin();
}
