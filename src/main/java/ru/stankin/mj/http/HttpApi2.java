package ru.stankin.mj.http;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.user.User;
import ru.stankin.mj.model.user.UserDAO;

@Path("/api2")
public class HttpApi2 {

//for testing with Curl curl -X POST -H "Content-Type: application/x-www-form-urlencoded; charset=utf-8" --data "student=114496&password=114496&semester=2015-%D0%B2%D0%B5%D1%81%D0%BD%D0%B0" http://localhost:8080/mj/webapi/api2/marks

    @Inject
    private Storage storage;

    @Inject
    private UserDAO userDAO;

    private final Response error401 = Response.status(401).build();

    private static Logger log = LogManager.getLogger(HttpApi2.class);

    // http://localhost:8080/mj/webapi/api2/marks?student=114531&password=114531&semester=w
    @POST
    @Path("marks")
    @Produces("application/json; charset=UTF-8")
    public Object marks(
            @FormParam("student") String cardId,
            @FormParam("password") String password,
            @FormParam("semester") String semester
    ){

        log.debug("marks requested cards={} password={} semester={}", cardId, password, semester);

        User s = userDAO.getUserBy(cardId, password);
        if (s == null)
            return error401;
        
        List<Module> modules = storage.getStudentById(((Student)s).id, semester).getModules();
        log.debug("modules are {}", modules);
        if (modules == null || modules.isEmpty())
            return modules;

        return modules.stream().map((m) ->
                new ModuleWrapper(m.getSubject().getTitle(),
                        m.getNum(), m.getValue(), m.getSubject().getFactor())
        ).collect(Collectors.toList());
    }

    @POST
    @Path("semesters")
    @Produces("application/json; charset=UTF-8")
    public Object semesters(
            @FormParam("student") String cardId,
            @FormParam("password") String password
    ) {
        
        User s = userDAO.getUserBy(cardId, password);
        if (s == null)
            return error401;
        
        Set<String> semesters = storage.getStudentSemestersWithMarks(((Student) s).id);
        Student student = (Student) s;
        
        return new SemestersWithSurnameWrapper(semesters, student.surname,
                    student.initials, student.stgroup);
    }
}

class ModuleWrapper {

    private final double factor;
    private String title;
    private String num;
    private int value;

    public ModuleWrapper(String title, String num, int value, double factor) {
        this.title = title;
        this.num = num;
        this.value = value;
        this.factor = factor;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public double getFactor() {
        return factor;
    }
}

class SemestersWithSurnameWrapper {
    
    private Set<String> semesters;
    private String surname;
    private String initials;
    private String stgroup;

    public SemestersWithSurnameWrapper(Set<String> semesters, String surname, String initials, String stgroup) {
        this.semesters = semesters;
        this.surname = surname;
        this.initials = initials;
        this.stgroup = stgroup;
    }

    public Set<String> getSemesters() {
        return semesters;
    }

    public void setSemesters(Set<String> semesters) {
        this.semesters = semesters;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getStgroup() {
        return stgroup;
    }

    public void setStgroup(String stgroup) {
        this.stgroup = stgroup;
    }
}