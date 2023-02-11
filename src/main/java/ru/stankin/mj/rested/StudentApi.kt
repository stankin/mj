package ru.stankin.mj.rested

import org.sql2o.Sql2o
import ru.stankin.mj.rested.security.MjRoles
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Created by nickl on 12.11.16.
 */


@Path("api3/student")
@Produces("application/json; charset=UTF-8")
open class StudentApi {

    @Inject
    private lateinit var sql2o: Sql2o


    @Path("login")
    @GET
    open fun login() = Response.ok("OK").build()

    @Path("students")
    @GET
    open fun students(): MutableList<Student>? {
//        SecurityUtils.getSubject().login()
        println("StudentApi: students: ${MjRoles.userAsStudent.id}")
        return sql2o.open().use { it.createQuery("SELECT CARDID, NAME, SURNAME FROM STUDENT").executeAndFetch(Student::class.java) }
    }

}



data class Student(val cardId: String, val name: String, val surname: String)

