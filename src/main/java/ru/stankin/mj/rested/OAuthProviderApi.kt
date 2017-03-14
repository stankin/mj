package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import org.apache.oltu.oauth2.`as`.request.OAuthTokenRequest
import org.apache.oltu.oauth2.common.validators.OAuthValidator
import org.sql2o.Sql2o
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.Student
import ru.stankin.mj.oauthprovider.OAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
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

    @POST
    @Path("/token")
    fun getUserInfo(@FormParam("code") code: Long,
                    @FormParam("client_secret") secret: String,
                    @FormParam("client_id") clientId: String): Response {


//    @POST
//    @Path("/getInfo")
//    fun getUserInfo(@Context request: HttpServletRequest): Response {
//
//        log.debug("getUserInfo params:" + request.parameterNames.toList())
//
//        val oauthRequest = object :OAuthTokenRequest(request)
//
//        val code = oauthRequest.code.toLong()
//        val clientId = oauthRequest.clientId
//        val secret = oauthRequest.clientSecret


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