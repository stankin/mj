package ru.stankin.mj.rested

import ru.stankin.mj.model.StudentsStorage
import ru.stankin.mj.rested.security.MjRoles
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam

/**
 * Created by nickl on 12.11.16.
 */


@Path("api3/student")
@Produces("application/json; charset=UTF-8")
open class StudentApi {

    @Inject
    private lateinit var storage: StudentsStorage

    @Path("marks")
    @GET
    open fun marks(@QueryParam("sem") sem: String): Any? {
        val student = storage.getStudentById(MjRoles.userAsStudent.id, sem) ?: throw NotFoundException()
        return mapOf(
            "group" to student.stgroup,
            "name" to student.name,
            "surname" to student.surname,
            "patronym" to student.patronym,
            "initials" to student.initials,
            "modules" to student.modules.groupBy { it.subject }.map { (s, ms) ->
                mapOf(
                    "subject" to s.let { mapOf("title" to it.title, "factor" to it.factor) },
                    "marks" to ms.associateBy({ markTypesMapping[it.num] ?: it.num }, { m ->
                        mapOf(
                            "value" to m.value,
                            "color" to m.color,
                        )
                    })
                )
            },
        )
    }

    @Path("semesters")
    @GET
    open fun sems(): Collection<String> {
        val student = MjRoles.userAsStudent
        return storage.getStudentSemestersWithMarks(student.id)
    }

}

private val markTypesMapping =
    mapOf("М1" to "module1", "М2" to "module2", "З" to "credit", "Э" to "exam", "К" to "courseWork")

