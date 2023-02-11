package ru.stankin.mj

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.StudentApi
import ru.stankin.mj.rested.security.ShiroListener
import ru.stankin.mj.testutils.InWeldWebTest
import ru.stankin.mj.testutils.MockableShiroFilter
import java.net.HttpURLConnection
import javax.ws.rs.core.HttpHeaders

class WebTest : InWeldWebTest() {


    private val log = LogManager.getLogger(WebTest::class.java)

    override fun restClasses() = listOf(StudentApi::class.java)
    override fun filters() = listOf(filter<MockableShiroFilter>("/*"))
    override fun listeners() = listOf(listener<ShiroListener>())
    override val restApiPrefix: String get() = "webapi"

    init {
        test("api request") {
            val content = restURL("/api3/student/login")
                    .openConnection().getInputStream().use { it.reader().readText() }
            content shouldBe "OK"
        }  
        
        test("api student") {
            val userResolver = bean<UserResolver>()
            val student = Student("OAuthStudent", "1", "2", "3", "4")
            userResolver.saveUser(student)
            val provider = bean<OAuthProvider>()
            val (clientId, secret) = provider.registerConsumer("testService", "some@some.com", emptyList())
            val permission = provider.addUserPermission(clientId, student.id.toLong())
            val connection = restURL("/api3/student/students")
                .openConnection() as HttpURLConnection
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer ${permission}")
            connection.responseCode shouldBe 200
            val content = connection.getInputStream().use { it.reader().readText() }
            content shouldBe "OK"
        }    
        
        test("api student unauthorised") {
            val connection = restURL("/api3/student/students").openConnection() as HttpURLConnection
            connection.responseCode shouldBe 401
        }



    }
}


