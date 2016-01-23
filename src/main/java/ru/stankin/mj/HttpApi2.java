package ru.stankin.mj;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
/**
 * Created by nickl-mac on 23.01.16.
 */
@Path("/api2")
public class HttpApi2 {

    @Inject
    private Storage storage;

    @Inject
    private UserDAO userDAO;

    ObjectMapper objectMapper = new ObjectMapper();

    @GET
    @Path("marks")
    @Produces("application/json; charset=UTF-8")
    public Object marks(
            @QueryParam("student") String cardId,
            @QueryParam("password") String password,
            @QueryParam("semester") String semester
    ){

        User s = userDAO.getUserBy(cardId, password);
        return storage.getStudentById(((Student)s).id, semester).getModules();

    }


}
