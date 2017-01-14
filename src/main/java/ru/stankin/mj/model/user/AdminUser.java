package ru.stankin.mj.model.user;

import java.io.Serializable;


public class AdminUser implements Serializable, User {

    private int id;

    private String username = null;
    private String email = null;
    private boolean isAdmin = true;

    public AdminUser() {
    }

    public AdminUser(String username, String email) {
        this.username = username;
        this.email = email;
    }


    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean isAdmin() {
        return isAdmin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int hashCode() {
        return (int) id;
    }

    public boolean equals(Object o) {
        if (o instanceof AdminUser) {
            return ((AdminUser) o).getId() == getId();
        }
        return false;
    }

    @Override
    public String toString() {
        return "AdminUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", isAdmin=" + isAdmin +
                '}';
    }

}