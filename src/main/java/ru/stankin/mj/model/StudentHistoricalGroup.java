package ru.stankin.mj.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "GroupsHistory", uniqueConstraints =
@UniqueConstraint(columnNames = {"student_id", "semestr", "groupName"}))
public class StudentHistoricalGroup implements Serializable {

    public StudentHistoricalGroup() {
    }

    public StudentHistoricalGroup(Student student, String semestr, String groupName) {
        this.groupName = groupName;
        this.semestr = semestr;
        this.student = student;
    }

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id = 0;

    @ManyToOne()
    @JoinColumn(name = "student_id")
    //@OnDelete(action = OnDeleteAction.CASCADE)
    public Student student;

    public String semestr;

    public String groupName;


}
