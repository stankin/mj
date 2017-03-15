package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.utils.restutils.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response


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
                  @Context request: HttpServletRequest
    ): Response {

        log.debug("request:{}", request.requestURI)

        if (responseType.toLowerCase() != "code") {
            return badRequest("response_type shoule be 'code'")
        }

        val consumer = prov.getConsumer(clientId)
        log.debug("consumer = {}", consumer)

        if (consumer == null || consumer.redirects.none { redirect.startsWith(it) })
            return badRequest("client does not exist or redirect is not registred")

        val user = MjRoles.getUser()

        log.debug("got user:{}", user)

        if (user == null) {
            SecurityUtils.getSubject().session.setAttribute("redirectAfterLogin", request.requestURI)
            return request.baseRedirect("/")
        }

        if (force == "yes" || prov.getSavedToken(clientId, user.id.toLong()) == null) {
            SecurityUtils.getSubject().session.setAttribute("redirectAfterLogin", request.requestURI)
            return request.baseRedirect("/givepermission") { queryParam("service", clientId) }

        }

        val code = prov.makeUserTemporaryCode(clientId, user.id.toLong())

        return redirect(redirect) {
            queryParam("code", code)
            queryParam("state", state)
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

        val user = userResolver.getUserById(userId.toInt())
        log.debug("writing responde for {} got by {}", user, userId)

        val tokenInfo = mutableMapOf<String, Any>(
                "access_token" to token,
                "token_type" to "bearer"
        )

        if (user is Student) {
            tokenInfo.put("userInfo", mapOf(
                    "name" to user.name,
                    "surname" to user.surname,
                    "patronym" to user.patronym,
                    "stgroup" to user.stgroup,
                    "cardid" to user.cardid
            ))
        }

        return Response.ok()
                .entity(tokenInfo)
                .build()

    }


}

