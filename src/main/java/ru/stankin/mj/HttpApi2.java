package ru.stankin.mj;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import javax.ws.rs.*;
import ru.stankin.mj.model.Module;

@Path("/api2")
public class HttpApi2 {

    @Inject
    private Storage storage;

    @Inject
    private UserDAO userDAO;

    ObjectMapper objectMapper = new ObjectMapper();

    // http://localhost:8080/mj/webapi/api2/marks?student=114531&password=114531&semester=w
    @GET
    @Path("marks")
    @Produces("text/plain; charset=UTF-8")
    public Object marks(
            @QueryParam("student") String cardId,
            @QueryParam("password") String password,
            @QueryParam("semester") String semester
    ){

        User s = userDAO.getUserBy(cardId, password);

        List<Module> modules = storage.getStudentById(((Student)s).id, semester).getModules();
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        for (Module m: modules) {
            sb.append(m.toJSON()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        
        return sb.toString();
//        return storage.getStudentById(((Student)s).id, semester).getModules();
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
        return storage.getStudentSemestersWithMarks(((Student) s).id);
    }
}
