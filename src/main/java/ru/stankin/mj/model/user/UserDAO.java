package ru.stankin.mj.model.user;

import java.util.List;

public interface UserDAO {

    @Deprecated
    public User getUserBy(String username, String password);

    public User getUserBy(String username);

    public boolean saveUser(User user);

    public boolean saveUserAndPassword(User user, String password);

    User getUserByPrincipal(Object principal);
}
