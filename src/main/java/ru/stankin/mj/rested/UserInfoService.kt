package ru.stankin.mj.rested

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.User
import ru.stankin.mj.oauthprovider.OAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

@Singleton
@Path("user")
@Produces("application/json; charset=UTF-8")
class UserInfoService {

    private val log = LogManager.getLogger(UserInfoService::class.java)

    @Inject
    private lateinit var prov: OAuthProvider

    @Inject
    private lateinit var userResolver: UserResolver

    @POST
    @GET
    @Path("/info")
    fun userInfo(@Context headers: HttpHeaders): Response {
        val userById = userByBearer(headers)

        log.debug("userInfo: {}", userById)

        return Response.ok()
                .entity(userInfo(userById))
                .build()
    }

    private fun userByBearer(headers: HttpHeaders): User {
        val authorizationHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION)
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw NotAuthorizedException("Authorization header required")
        }

        val token = authorizationHeader.removePrefix("Bearer").trim()
        val userId = prov.getUserIdByToken(token) ?: throw NotAuthorizedException("invalid token")
        val userById = userResolver.getUserById(userId) ?: throw NotAuthorizedException("user not found")
        return userById
    }

    companion object {
        fun userInfo(user: User): Map<String, Any> {
            return when(user){
                is Student -> mapOf(
                        "name" to user.name,
                        "surname" to user.surname,
                        "patronym" to user.patronym,
                        "stgroup" to user.stgroup,
                        "cardid" to user.cardid
                )
                is AdminUser -> mapOf(
                        "admin" to user.isAdmin,
                        "username" to user.username
                )

                else -> mapOf()
            }

        }
    }
}