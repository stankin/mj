package ru.stankin.mj

import io.buji.pac4j.subject.Pac4jPrincipal
import io.buji.pac4j.token.Pac4jToken
import io.kotlintest.matchers.be
import io.undertow.Handlers
import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.support.DelegatingSubject
import org.apache.shiro.web.mgt.WebSecurityManager
import org.jboss.weld.environment.se.Weld
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.StudentsStorage
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.rested.security.YandexProfile
import ru.stankin.mj.testutils.InWeldTest
import java.io.PrintWriter
import javax.inject.Inject
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import io.undertow.Undertow
import io.undertow.server.handlers.PathHandler
import io.undertow.servlet.Servlets.*
import org.jboss.`as`.server.deployment.DeploymentHandlerUtil.deploy
import io.undertow.servlet.api.DeploymentManager
import io.undertow.servlet.api.DeploymentInfo
import io.undertow.servlet.api.InstanceFactory
import io.undertow.servlet.api.ServletInfo
import io.undertow.servlet.util.ImmediateInstanceFactory
import ru.stankin.mj.testutils.InWeldWebTest
import java.net.URL
import java.net.URLConnection


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

    init {
        test("Web server on top of weld") {
            val content = URL("http://localhost:8080/myservlet")
                    .openConnection().getInputStream().use { it.reader().readText() }

            content shouldBe "students:0\n"
        }


    }
}


