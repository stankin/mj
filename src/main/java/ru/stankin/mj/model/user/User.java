package ru.stankin.mj.model.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by nickl on 16.02.15.
 */
public interface User {
    @NotNull
    String getUsername();

    @Nullable
    String getEmail();

    int getId();

    boolean isAdmin();

}
