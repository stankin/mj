package ru.stankin.mj;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import ru.stankin.mj.model.Module;

@Path("/api2")
public class HttpApi2 {

    @Inject
    private Storage storage;

    @Inject
    private UserDAO userDAO;

    ObjectMapper objectMapper = new ObjectMapper();
    
    private final Response error401 = Response.status(401).build();

    // http://localhost:8080/mj/webapi/api2/marks?student=114531&password=114531&semester=w
    @GET
    @Path("marks")
    @Produces("application/json; charset=UTF-8")
    public Object marks(
            @QueryParam("student") String cardId,
            @QueryParam("password") String password,
            @QueryParam("semester") String semester
    ){

        User s = userDAO.getUserBy(cardId, password);
        if (s == null)
            return error401;
        
        List<Module> modules = storage.getStudentById(((Student)s).id, semester).getModules();        
        if (modules == null || modules.isEmpty())
            return modules;
        
        List<ModuleWrapper> moduleWrappers = new ArrayList<>();
        modules.stream().forEach((m) -> {
            moduleWrappers.add(new ModuleWrapper(m.getSubject().getTitle(),
                    m.getNum(), m.getValue()));
        });
                
        return moduleWrappers;
    }

    // http://localhost:8080/mj/webapi/api2/semesters?student=114531&password=114531
    @GET
    @Path("semesters")
    @Produces("application/json; charset=UTF-8")
    public Object semesters(
            @QueryParam("student") String cardId,
            @QueryParam("password") String password
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

    private String title;
    private String num;
    private int value;

    public ModuleWrapper(String title, String num, int value) {
        this.title = title;
        this.num = num;
        this.value = value;
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