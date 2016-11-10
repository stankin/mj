package ru.stankin.mj.model;

import ru.stankin.mj.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nickl on 08.01.15.
 */
@Entity
@Table(name = "Student", indexes = {
        @Index(columnList = "stgroup, surname, initials") ,
        @Index(columnList = "cardid")
})
public class Student implements Serializable, User, Comparable {

    private static final long serialVersionUID = 1L;
    public final static Comparator<Student> studentComparator = Comparator.comparing((Student s) -> s.stgroup)
            .thenComparing(s -> s.surname)
            .thenComparing(s -> s.initials)
            .thenComparing(s -> s.cardid);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int id = 0;

    public String stgroup;
    public String surname;
    public String initials;

    public String password;

    //@ElementCollection
    @Transient
    //@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "student")
    private List<Module> modules = new ArrayList<>();
    public String name;
    public String patronym;

    @Column(unique = true)
    public String cardid;

    public Student() {
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "student")
    private List<StudentHistoricalGroup> groups = new ArrayList<>();

    public Student(String group, String surname, String initials) {
        this.stgroup = group;
        this.surname = surname;
        this.initials = initials;
    }

    public Optional<StudentHistoricalGroup> getHistoricalGroup(String semester) {
        return getGroups().stream().filter(g -> g.semestr.equals(semester)).findAny();
    }

    public void initialsFromNP() {
        if (initials != null)
            return;
        if (name == null || surname == null)
            return;

        initials = initialsFromNames(name, patronym);
    }

    public static String initialsFromNames(String name, String patronym) {
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

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    @Override
    public String getUsername() {
        return cardid;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
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
}
