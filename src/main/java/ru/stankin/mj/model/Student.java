package ru.stankin.mj.model;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.stankin.mj.model.user.User;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nickl on 08.01.15.
 */

public class Student implements Serializable, User, Comparable {

    private static final long serialVersionUID = 1L;
    public final static Comparator<Student> studentComparator = Comparator.comparing((Student s) -> s.stgroup)
            .thenComparing(s -> s.surname)
            .thenComparing(s -> s.initials)
            .thenComparing(s -> s.cardid);

    public int id = 0;

    @Nullable
    public String stgroup;
    @Nullable
    public String surname;
    @Nullable
    public String initials;

    @NotNull
    private List<Module> modules = new ArrayList<>();

    @Nullable
    public String name;

    @Nullable
    public String patronym;

    @Nullable
    public String cardid;

    @Nullable
    public String email;


    public Student() {
    }

    private List<StudentHistoricalGroup> groups = new ArrayList<>();

    public Student(@Nullable String group,@Nullable String surname, @Nullable String initials) {
        this.stgroup = group;
        this.surname = surname;
        this.initials = initials;
    }

    public Student(@Nullable String cardid, String group, String surname, String initials) {
        this(group, surname, initials);
        this.cardid = cardid;
    }

    public Student(@Nullable String cardid, @Nullable String group, @Nullable String surname,
                   @Nullable String name, @Nullable String patronym) {
        this(cardid, group, surname, initialsFromNames(name, patronym));
        this.name = name;
        this.patronym = patronym;
    }

    public Student(String cardid) {
        this(cardid, "", "", "");
    }

    public Optional<StudentHistoricalGroup> getHistoricalGroup(String semester) {
        return getGroups().stream().filter(g -> g.semestr.equals(semester)).findAny();
    }

    public void initialsFromNP() {
        if (initials != null)
            return;
        if (name == null || patronym == null)
            return;

        initials = initialsFromNames(name, patronym);
    }

    @NotNull
    public static String initialsFromNames(@NotNull String name, @NotNull String patronym) {
        String sp = "";
        if (patronym.length() > 0)
            sp = Character.toUpperCase(patronym.charAt(0)) + ".";
        return Character.toUpperCase(name.charAt(0)) + "." + sp;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                " card='" + cardid + '\'' +
                ", '" + stgroup + '\'' +
                ", '" + surname + '\'' +
                ", '" + initials + '\'' +
                ", '" + name + '\'' +
                ", '" + patronym + '\'' +
                ", " + getModules() +
                '}';
    }

    @Override
    public int compareTo(Object o) {
          return  studentComparator.compare(this, (Student) o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Student student = (Student) o;

        if (stgroup != null ? !stgroup.equals(student.stgroup) : student.stgroup != null) return false;
        if (initials != null ? !initials.equals(student.initials) : student.initials != null) return false;
        if (surname != null ? !surname.equals(student.surname) : student.surname != null) return false;
        if (cardid != null ? !cardid.equals(student.cardid) : student.cardid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stgroup != null ? stgroup.hashCode() : 0;
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + (initials != null ? initials.hashCode() : 0);
        result = 31 * result + (cardid != null ? cardid.hashCode() : 0);
        return result;
    }

    @NotNull
    public List<Module> getModules() {
        return modules;
    }

    public Map<Subject, Map<String, Module>> getModulesGrouped() {
        return getModules().stream().collect(
                Collectors.groupingBy(Module::getSubject,
                        Collectors.groupingBy(Module::getNum,
                                Collectors.reducing(((Module) null), (Module a, Module b) -> b)
                        )));
    }

    public void setModules(@NotNull List<Module> modules) {
        this.modules = modules;
        modules.forEach(m -> m.studentId = this.id);
    }

    @Override
    @NotNull
    public String getUsername() {
        return  cardid != null ? cardid : "no_cardid_set";
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }


    public List<StudentHistoricalGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<StudentHistoricalGroup> groups) {
        this.groups = groups;
    }

    @Override
    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }
}
