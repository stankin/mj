package ru.stankin.mj

import io.kotlintest.matchers.be
import org.apache.logging.log4j.LogManager
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.common.message.types.GrantType
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.OAuthProviderService
import ru.stankin.mj.rested.UserInfoService
import ru.stankin.mj.rested.security.ShiroListener
import ru.stankin.mj.testutils.InWeldWebTest
import ru.stankin.mj.testutils.Matchers.ne
import ru.stankin.mj.testutils.MockableShiroFilter
import ru.stankin.mj.utils.JSON
import ru.stankin.mj.utils.restutils.queryParams
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.HttpHeaders


@Throws(IOException::class)
fun doRequest(req: OAuthClientRequest): HttpURLConnection {
    val url = URL(req.getLocationUri())
    val c = url.openConnection() as HttpURLConnection
    c.instanceFollowRedirects = true
    c.connect()
    c.responseCode
    return c
}

@Throws(IOException::class)
fun doPostRequest(req: OAuthClientRequest): HttpURLConnection {
    val url = URL(req.getLocationUri())
    val c = url.openConnection() as HttpURLConnection
    c.instanceFollowRedirects = true
    c.doOutput = true
    c.requestMethod = "POST"
    c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    c.setRequestProperty("Content-Length", req.body.length.toString());
    LogManager.getLogger(OAuth2ProvidingTest::class.java).debug("doPostRequest: body: {}", req.body)
    c.outputStream.writer().use { it.write(req.body) }
    c.responseCode
    return c
}

class OAuth2ProvidingTest : InWeldWebTest() {

    private val log = LogManager.getLogger(OAuth2ProvidingTest::class.java)

    override fun restClasses() = listOf(OAuthProviderService::class.java, UserInfoService::class.java)

    override fun filters() = listOf(
            filter<MockableShiroFilter>("/*")
    )

    override fun listeners() = listOf(listener<ShiroListener>())

    init {

        test("Oauth redirect user authorize") {
            val provider = bean<OAuthProvider>()
            val (clientId, secret) = provider.registerConsumer("testService1", "som1e@some.com", listOf("http://example.com/"))
            val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                    .setResponseType("code")
                    .setClientId(clientId)
                    .setRedirectURI("http://example.com/login")
                    .setState("abc").buildQueryMessage()

            val c = doRequest(request)
            c.responseCode  should be ne 500
            c.url.toString() shouldBe "http://localhost:8080/"
            log.debug("c url= {}", c.responseCode)
            log.debug("c url= {}", c.url)
        }

        test("Oauth unregistred redirect user authorize") {
            val provider = bean<OAuthProvider>()
            val (clientId, secret) = provider.registerConsumer("testServiceWithInvalidRedirect", "som1e@some.com", listOf("http://yandex.ru"))
            val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                    .setResponseType("code")
                    .setClientId(clientId)
                    .setRedirectURI("http://example.com/login")
                    .setState("abc").buildQueryMessage()

            val c = doRequest(request)
            c.responseCode  shouldBe 404
            c.url.host shouldBe "example.com"
            c.url.queryParams["error"] shouldBe "client+does+not+exist+or+redirect+is+not+registered"
            log.debug("c url= {}", c.responseCode)
            log.debug("c url= {}", c.url)
        }

        test("Oauth user authorize without permission") {
            val provider = bean<OAuthProvider>()
            val userResolver = bean<UserResolver>()
            val student = Student("OAuthStudentWithoutPermission", "1", "2", "3", "4")
            userResolver.saveUser(student)
            val (clientId, secret) =
                    provider.registerConsumer("testService3", "som2e@some.com", listOf("]); DROP TABLE student;--", "http://example.com/","http://example1.com/"))

            MockableShiroFilter.runAs(student) {
                val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                        .setResponseType("code")
                        .setClientId(clientId)
                        .setRedirectURI("http://example.com/login")
                        .setState("abc").buildQueryMessage()

                val c = doRequest(request)
                c.responseCode  should be ne 500
                c.url.host shouldBe "localhost"
                c.url.path shouldBe "/"
                c.url.ref shouldBe "!givepermission"
                c.url.queryParams["service"] shouldBe clientId
            }

        }

        test("Oauth user authorize") {
            val provider = bean<OAuthProvider>()
            val userResolver = bean<UserResolver>()
            val student = Student("OAuthStudent1", "1", "2", "3", "4")
            userResolver.saveUser(student)
            val (clientId, secret) =
                    provider.registerConsumer("testService2", "som2e@some.com", listOf("http://example.com/","http://example1.com/"))

            provider.addUserPermission(clientId, student.id.toLong())

            MockableShiroFilter.runAs(student) {
                val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                        .setResponseType("code")
                        .setClientId(clientId)
                        .setRedirectURI("http://example.com/login")
                        .setState("abc").buildQueryMessage()

                val c = doRequest(request)
                c.responseCode  should be ne 500
                c.url.host shouldBe "example.com"
                c.url.path shouldBe "/login"
                c.url.queryParams["state"] shouldBe "abc"
                val code = c.url.queryParams["code"]!!.toLong()

                val user = provider.resolveByTemporaryCode(code)!!

                user.clientId shouldBe clientId
                user.userId.toInt() shouldBe student.id
            }

            // Станет недоступен после удаления
            provider.removeUserPermission(clientId, student.id.toLong())

            MockableShiroFilter.runAs(student) {
                val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                        .setResponseType("code")
                        .setClientId(clientId)
                        .setRedirectURI("http://example.com/login")
                        .setState("abc").buildQueryMessage()

                val c = doRequest(request)
                c.responseCode  should be ne 500
                c.url.host shouldBe "localhost"
                c.url.path shouldBe "/"
                c.url.ref shouldBe "!givepermission"
                c.url.queryParams["service"] shouldBe clientId
            }

        }

        test("Oauth token") {
            val userResolver = bean<UserResolver>()
            val student = Student("OAuthStudent", "1", "2", "3", "4")
            userResolver.saveUser(student)
            val provider = bean<OAuthProvider>()
            val (clientId, secret) = provider.registerConsumer("testService", "some@some.com", emptyList())
            val permission = provider.addUserPermission(clientId, student.id.toLong())

            log.debug("student {}", student)

            val code = provider.makeUserTemporaryCode(clientId, student.id.toLong())!!
            val request = OAuthClientRequest.TokenRequestBuilder(restURL("/oauth/token").toString())
                    .setClientId(clientId)
                    .setClientSecret(secret)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setCode(code.toString())
                    .buildBodyMessage()

            val c = doPostRequest(request)

            val jsonResponse = c.inputStream.reader().readText()

            log.debug("jsonResponse={}", jsonResponse)

            val body = JSON.read<Map<String, Any>>(jsonResponse)

            body shouldBe mapOf("access_token" to permission,
                    "token_type" to "bearer",
                    "userInfo" to mapOf(
                            "name" to student.name,
                            "surname" to student.surname,
                            "patronym" to student.patronym,
                            "stgroup" to student.stgroup,
                            "cardid" to student.cardid
                    )
            )

            // Аналогично через http

            val c1 = restURL("/user/info").openConnection() as HttpURLConnection
            c1.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer ${permission}");
            c1.connect()
            c1.responseCode  shouldBe 200
            val studentInfo = JSON.read<Map<String, Any>>(c1.inputStream)
            studentInfo["name"] shouldBe student.name
            studentInfo["cardid"] shouldBe student.cardid

        }

        test("Bad request") {

            val request = OAuthClientRequest.authorizationLocation(restURL("/oauth/authorize").toString())
                    .setRedirectURI("http://example.com/login")
                    .setState("abc").buildQueryMessage()

            val c = doRequest(request)
            c.responseCode should be ne 500
            c.url.host shouldBe "example.com"
            c.url.path shouldBe "/login"
            c.url.ref shouldBe null
            c.url.queryParams["error"] shouldBe "param+%27response_type%27+cant+be+null"

        }



    }


}
