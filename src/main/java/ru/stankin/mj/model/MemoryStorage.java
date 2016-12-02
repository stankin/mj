package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.inject.Alternative;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by nickl on 08.01.15.
 */

@Alternative
public class MemoryStorage implements Storage {

    private static final Logger logger = LogManager.getLogger(MemoryStorage.class);
    private final boolean etalonless;

    private TreeSet<Student> students = new TreeSet<>();
    private ArrayList<Student> studentsList = new ArrayList<>();

    public MemoryStorage(boolean etalonless) {
        this.etalonless = etalonless;
    }

    public MemoryStorage() {
        this(false);
    }

    @Override
    public synchronized void updateModules(Student student) {
        //System.out.println("updateModules:"+student);
        if (!addStudent(student)) {
            Student floor = students.floor(student);
            //logger.debug("floor: {}", floor);
            floor.setModules(student.getModules());
        }

        //logger.debug("studentscount: {}",students.size());
    }

    private synchronized boolean addStudent(Student student) {
        boolean add = students.add(student);
        if (add) {
            student.id = students.size() - 1;
            studentsList.add(student);
        }
        return add;
    }

    @Override
    public Stream<Student> getStudents() {
        return students.stream();
    }

    @Override
    public Stream<Student> getStudentsFiltred(String text) {
        //logger.debug("students: {}",students);
        return students.stream().filter(s ->
                s.stgroup.contains(text) ||
                s.surname.contains(text) ||
                s.initials.contains(text)
        );
    }

    @Override
    public Student getStudentById(int id, String semester) {
        return studentsList.get(id);
    }

    @Override
    public Set<String> getStudentSemestersWithMarks(int student) {
        throw new UnsupportedOperationException("getStudentSemestersWithMarks");
    }

    @Override
    public Set<String> getKnownSemesters() {
        throw new UnsupportedOperationException("getKnownSemesters");
    }

    @Override
    public Student getStudentByCardId(String cardid) {

        if (etalonless) {
            if (cardid.equals(""))
                throw new IllegalArgumentException("empty cardid");


            return studentsList.stream().filter(s -> s.cardid.equals(cardid)).findAny().orElseGet(() -> {
                Student student = new Student("nogroup", "noname", "noinitials");
//            if (cardid.equals("")) {
//                student.cardid = Integer.toString(students.size() - 1);
//            } else {
                student.cardid = cardid;
                //}
                addStudent(student);
                return student;
            });
        } else return null;
    }

    @Override
    public void deleteStudentModules(Student student, String semester) {
        throw new UnsupportedOperationException("deleteStudentModules");
    }

    @Override
    public void saveStudent(Student student, String semestr) {
        addStudent(student);
    }

    private Map<String, Subject> stringSubjectMap = new HashMap<>();

    @Override
    public Subject getOrCreateSubject(String semester, String group, String name, double factor) {
        String id = semester + "-" + group + "-" + name;
        Subject subject = stringSubjectMap.get(id);
        if(subject == null){
            subject = new Subject(semester, group, name, factor);
            stringSubjectMap.put(id, subject);
        }
        return subject;
    }

    @Override
    public Student getStudentByGroupSurnameInitials(String semestr, String group, String surname, String initials) {
        return new Student(group, surname, initials);
    }

    @Override
    public void deleteAllModules(String semestr) {
        throw new UnsupportedOperationException("deleteAllModules");
    }

    @Override
    public void deleteStudent(Student s) {
        throw new UnsupportedOperationException("deleteStudent");
    }

}
