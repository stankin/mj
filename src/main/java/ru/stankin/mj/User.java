package ru.stankin.mj;

import java.io.Serializable;

public class User implements Serializable {
    private final long id;

    private String firstName = "anonymous";
    private String lastName = "user";
    private String username = null;
    private String password = null;
    private String email = null;
    private boolean isAdmin = false;

    public User() {
        id = -1;
    }

    public User(long id, String username, String password, String firstName,
                String lastName, String email, boolean isAdmin) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public long getId() {
        return id;
    }

    public int hashCode() {
        return (int) id;
    }

    public boolean equals(Object o) {
        if (o instanceof User) {
            return ((User) o).getId() == getId();
        }
        return false;
    }
}