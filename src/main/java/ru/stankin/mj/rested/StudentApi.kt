package ru.stankin.mj.rested

import ru.stankin.mj.model.user.UserDAO
import javax.annotation.Resource
import javax.inject.Inject
import javax.sql.DataSource
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import kotlinx.support.jdk7.use
import java.sql.ResultSet
import kotlin.reflect.KFunction

/**
 * Created by nickl on 12.11.16.
 */


@Path("api3/student")
@Produces("application/json; charset=UTF-8")
open class StudentApi {

    @Inject
    private lateinit var ds: DataSource


    @Path("login")
    @GET
    open fun login() = Response.ok("OK").build()

    @Path("students")
    @GET
    open fun students() = ds.connection.use {
        it.createStatement().use {
            it.executeQuery("SELECT * FROM STUDENT").map {
                Student(cardId = it.getString("cardid"), name = it.getString("NAME"), surname = it.getString("surname"))
            }.toList()
        }
    }

}


public fun <T : Any> ResultSet.map(f: (ResultSet) -> T) = generateSequence { ->
    if (this.next()) {
        f(this)
    } else null
}

data class Student(val cardId: String, val name: String, val surname: String)

fun <T : Any> paramsNamest(c: KFunction<T>) = c.parameters.map { it.name!!.toLowerCase() }.toSet()

public inline fun <reified T : Any> ResultSet.mapToClass(): Sequence<T> {

    val columns = 1.rangeTo(this.metaData.columnCount).associate { this.metaData.getColumnName(it).toLowerCase() to (it) }

    val columsNameSet = columns.keys

    val matched = T::class.constructors.map { c -> (paramsNamest(c) - columsNameSet).size to c }
    val cc = matched.filter { it.first == 0 }.maxBy { it.second.parameters.size }?.second

    if (cc == null) {
        throw IllegalArgumentException("No matching for:" + (paramsNamest(matched.minBy { it.first }!!.second) - columsNameSet))
    }

    return generateSequence { ->
        if (this.next()) {
            cc.call(*cc.parameters.map { p -> this.getString(columns[p.name!!.toLowerCase()]!!) }.toTypedArray())
        } else null
    }

}

