package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Response

/**
 * Created by nickl on 14.03.17.
 */

@Singleton
@Path("oauth")
@Produces("application/json; charset=UTF-8")
class OAuthProviderApi {

    private val log = LogManager.getLogger(OAuthProviderApi::class.java)

    @Inject
    private lateinit var prov: OAuthProvider

    @Inject
    private lateinit var userResolver: UserResolver

//    @GET
//    @Path("/authorize")
//    fun authorize(@QueryParam("response_type") responseType: String,
//                  @QueryParam("client_id") clientId: String,
//                  @QueryParam("force_confirm") force: String?,
//                  @QueryParam("state") state: String?
//    ): Response {
//
//        if (responseType.toLowerCase() != "code")
//            return Response.status(Response.Status.BAD_REQUEST).entity(mapOf("message" to "response_type shoule be 'code'")).build()
//
//
//
//
//
//
//    }

    @POST
    @Path("/token")
    fun getUserInfo(@FormParam("code") code: Long,
                    @FormParam("client_secret") secret: String,
                    @FormParam("client_id") clientId: String): Response {

        val (serviceName, userId, token) = prov.resolveByTemporaryCode(code) ?:
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to "nothing bound to this code"))
                        .build()

        if (prov.getConsumer(clientId, secret)?.serviceName != serviceName)
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