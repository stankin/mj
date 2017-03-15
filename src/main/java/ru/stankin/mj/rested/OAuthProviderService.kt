package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.security.MjRoles
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

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

        if (responseType.toLowerCase() != "code")
            return Response.status(Response.Status.BAD_REQUEST).entity(mapOf("message" to "response_type shoule be 'code'")).build()

        val consumer = prov.getConsumer(clientId)
        log.debug("consumer = {}", consumer)

        if(consumer == null || consumer.redirects.none { redirect.startsWith(it)  })
            return Response.status(Response.Status.BAD_REQUEST).entity(mapOf("message" to "client does not exist or redirect is not registred")).build()


        val user = MjRoles.getUser()

        log.debug("got user:{}", user)

        if (user == null) {
            SecurityUtils.getSubject().session.setAttribute("redirectAfterLogin", request.requestURI)
            return Response.temporaryRedirect(URI(request.requestURL.removeSuffix(request.requestURI).toString() + "/" + request.contextPath)).build()
        }


        val code = prov.makeUserTemporaryCode(clientId, user.id.toLong())

        val uri = UriBuilder.fromUri(redirect)
                .queryParam("code", code)
                .queryParam("state", state).build()

        return Response.temporaryRedirect(uri).build()

    }

    @POST
    @Path("/token")
    fun tokenAndInfo(@FormParam("code") code: Long,
                     @FormParam("client_secret") secret: String,
                     @FormParam("client_id") clientId: String): Response {

        val (resolvedClientId, userId, token) = prov.resolveByTemporaryCode(code) ?:
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to "nothing bound to this code"))
                        .build()

        val consumer = prov.getConsumer(clientId, secret)
        log.debug("consumer = {}", consumer)
        if (resolvedClientId != clientId || consumer == null)
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(mapOf("message" to "consumer authentication failed"))
                    .build()


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