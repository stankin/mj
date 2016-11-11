package ru.stankin.mj;

import ru.stankin.mj.model.User;

import javax.enterprise.inject.Alternative;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Alternative
public class TestingUserDAO implements UserDAO, Serializable {

    static List<User> users;

    static {
        users = new LinkedList<User>();
//        users.add(new AdminUser(0, "admin", "admin", "System", "Administrator",
//                null, true));
//        users.add(new AdminUser(1, "user", "user", "Some", "User", null, false));
//        users.add(new AdminUser(2, "ernest", "john", "Ernest", "Worthing", null,
//                false));
    }

    public TestingUserDAO() {

    }

    @Override
    public User getUserBy(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)
                    && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User getUserBy(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean saveUser(User user) {
        try {
            if (users.contains(user)) {
                return true;
            } else {
                users.add(user);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<User> getUsers() {
        return users;
    }

    @Override
    public User getUserCookie(String Cookie) {
        for (User user : users) {
            if (user.getCookie().equals(Cookie)) {
                return user;
            }
        }
        return null;
    }

}