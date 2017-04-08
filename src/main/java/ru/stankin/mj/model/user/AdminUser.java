package ru.stankin.mj.model.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;


public class AdminUser implements Serializable, User {

    private int id;

    @Nullable
    private String username = null;
    @Nullable
    private String email = null;
    private boolean isAdmin = true;

    public AdminUser() {
    }

    public AdminUser(@Nullable String username,@Nullable  String email) {
        this.username = username;
        this.email = email;
    }


    @Override
    @NotNull
    public String getUsername() {
        return username != null? username : "anonymous";
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
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