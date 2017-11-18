package ru.stankin.mj

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.StudentsStorage
import ru.stankin.mj.rested.StudentApi
import ru.stankin.mj.testutils.InWeldWebTest
import java.io.PrintWriter
import javax.inject.Inject
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Created by nickl on 11.01.17.
 */


class MyServlet : HttpServlet() {

    private val log = LogManager.getLogger(MyServlet::class.java)

    @Inject
    lateinit var students: StudentsStorage

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.setContentType("text/plain")
        PrintWriter(resp.writer).use { out ->
            out.println("students:" + students.students.use { it.count() })
        }

    }
}


class WebTest : InWeldWebTest() {


    private val log = LogManager.getLogger(WebTest::class.java)

    override fun servlets() = listOf(
            servlet<MyServlet>("/myservlet")
    )

    override fun restClasses() = listOf(StudentApi::class.java)

    init {
        test("Web server on top of weld") {

            val content = serverURL("/myservlet")
                    .openConnection().getInputStream().use { it.reader().readText() }

            content shouldBe "students:0\n"
        }

        test("api request") {
            val content = restURL("/api3/student/login")
                    .openConnection().getInputStream().use { it.reader().readText() }
            content shouldBe "OK"
        }



    }
}


