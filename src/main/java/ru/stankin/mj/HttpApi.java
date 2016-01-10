package ru.stankin.mj;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by nmitropo on 10.1.2016.
 */
@WebServlet(name = "HttpApi", urlPatterns = "api/marks")
public class HttpApi extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Inject
    private Storage storage;

    @Inject
    private UserDAO userDAO;

    ObjectMapper objectMapper = new ObjectMapper();


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String cardId = request.getParameter("student");
        String password = request.getParameter("password");
        String semester = request.getParameter("semester");

        User s = userDAO.getUserBy(cardId, password);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF8");
        try(PrintWriter writer = response.getWriter()){
            List<Module> modules = storage.getStudentById(((Student)s).id, semester).getModules();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, modules);
        }

    }
}
