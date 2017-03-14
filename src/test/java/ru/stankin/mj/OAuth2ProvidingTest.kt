package ru.stankin.mj

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.apache.oltu.oauth2.`as`.issuer.MD5Generator
import org.apache.oltu.oauth2.`as`.issuer.OAuthIssuerImpl
import org.apache.oltu.oauth2.`as`.request.OAuthAuthzRequest
import org.apache.oltu.oauth2.`as`.response.OAuthASResponse
import org.apache.oltu.oauth2.client.OAuthClient
import org.apache.oltu.oauth2.client.URLConnectionClient
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.common.OAuth
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.message.types.GrantType
import org.apache.oltu.oauth2.common.message.types.ResponseType
import org.apache.oltu.oauth2.common.utils.OAuthUtils
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.StudentsStorage
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.OAuthProviderApi
import ru.stankin.mj.testutils.InWeldWebTest
import java.io.IOException
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response


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


    override fun restClasses(): List<Class<*>> {
        return listOf(OAuthProviderApi::class.java)
    }

    init {
        test("Oauth providing") {


            val userResolver = bean<UserResolver>()

            val student = Student("OAuthStudent", "1", "2", "3", "4")
            userResolver.saveUser(student)

            val provider = bean<OAuthProvider>()

            val (token, secret) = provider.registerConsumer("testService", "some@some.com")
            val permission = provider.addUserPermission("testService", student.id.toLong())

            log.debug("student {}", student)


            val code = provider.makeUserTemporaryCode("testService", student.id.toLong())!!

            val request = OAuthClientRequest.TokenRequestBuilder(restURL("/oauth/token").toString())
                    .setClientId(token)
                    .setClientSecret(secret)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setCode(code.toString())
                    .buildBodyMessage()

            val c = doPostRequest(request)

            val body = ObjectMapper().readValue<Map<String, Any>>(c.inputStream, Map::class.java as Class<Map<String, Any>>)

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

        }


    }
}
