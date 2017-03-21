package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import org.jboss.resteasy.spi.BadRequestException
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.User
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.utils.IllegalRestParameterNullability
import ru.stankin.mj.utils.restutils.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.*
import javax.servlet.annotation.WebFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.constraints.NotNull
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.NotAuthorizedException
import kotlin.IllegalArgumentException


/**
 * Created by nickl on 14.03.17.
 */

@Singleton
@Path("oauth")
@Produces("application/json; charset=UTF-8")
class OAuthProviderService {

    private val log = LogManager.getLogger(OAuthProviderService::class.java)

    @Inject
    private lateinit var prov: OAuthProvider

    @Inject
    private lateinit var userResolver: UserResolver

    @GET
    @Path("/authorize")
    fun authorize(@QueryParam("response_type") responseType: String,
                  @QueryParam("client_id") clientId: String,
                  @QueryParam("redirect_uri") redirect: String,
                  @QueryParam("force_confirm") force: String?,
                  @QueryParam("state") state: String?,
                  @QueryParam("error") error: String?,
                  @Context request: HttpServletRequest
    ): Response {

        if(error != null)
            return redirect(redirect) {
                queryParam("error", error)
                queryParamNullable("state", state)
            }

        log.debug("request:{}", request.requestURI)

        if (responseType.toLowerCase() != "code") {
            throw BadRequestException("response_type should be 'code'")
        }

        val consumer = prov.getConsumer(clientId)
        log.debug("consumer = {}", consumer)

        if (consumer == null || consumer.redirects.none { redirect.startsWith(it) })
            throw BadRequestException("client does not exist or redirect is not registered")

        val user = MjRoles.getUser()

        log.debug("got user:{}", user)

        if (user == null) {
            SecurityUtils.getSubject().session.setAttribute("redirectAfterLogin", request.fullURI)
            return request.baseRedirect("/")
        }

        if (force == "yes" || prov.getSavedToken(clientId, user.id.toLong()) == null) {
            SecurityUtils.getSubject().session.setAttribute("redirectAfterLogin", request.fullURI)
            return request.baseRedirect("/") {
                fragment("!givepermission")
                queryParam("service", clientId)
            }

        }

        val code = prov.makeUserTemporaryCode(clientId, user.id.toLong())

        return redirect(redirect) {
            queryParam("code", code)
            queryParamNullable("state", state)
        }

    }

    @POST
    @Path("/token")
    fun tokenAndInfo(@FormParam("code") code: Long,
                     @FormParam("client_secret") secret: String,
                     @FormParam("client_id") clientId: String): Response {

        val (resolvedClientId, userId, token) = prov.resolveByTemporaryCode(code) ?:
                return responseMessage(Response.Status.NOT_FOUND, "nothing bound to this code")

        val consumer = prov.getConsumer(clientId, secret)
        log.debug("consumer = {}", consumer)
        if (resolvedClientId != clientId || consumer == null)
            return responseMessage(Response.Status.UNAUTHORIZED, "consumer authentication failed")

        val user: User = userResolver.getUserById(userId.toInt()) ?:
                return responseMessage(Response.Status.NOT_FOUND, "user is not found")
        log.debug("writing responde for {} got by {}", user, userId)

        val tokenInfo = mutableMapOf<String, Any>(
                "access_token" to token,
                "token_type" to "bearer"
        )

        tokenInfo.put("userInfo", UserInfoService.userInfo(user))

        return Response.ok()
                .entity(tokenInfo)
                .build()
    }

}



