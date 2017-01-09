package ru.stankin.mj.model.user;

import java.util.List;

public interface UserDAO {

    public User getUserBy(String username, String password);

    public User getUserBy(String username);

    public boolean saveUser(User user);

    User getUserByPrincipal(Object principal);
}
