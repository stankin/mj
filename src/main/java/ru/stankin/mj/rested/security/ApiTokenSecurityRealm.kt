package ru.stankin.mj.rested.security

import org.apache.shiro.authc.*
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.authz.UnauthorizedException
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.web.filter.authc.AuthenticatingFilter
import org.jboss.resteasy.util.HttpResponseCodes
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper


class ApiTokenSecurityRealm(
    private val userService: UserResolver,
    private val prov: OAuthProvider
) : AuthorizingRealm() {

    override fun supports(token: AuthenticationToken?): Boolean = token is BearerToken

    override fun doGetAuthenticationInfo(token: AuthenticationToken?): AuthenticationInfo {
        val bearer = (token as BearerToken).bearer
        val userId = prov.getUserIdByToken(bearer) ?: throw IncorrectCredentialsException("no user for given bearer")
        val user = userService.getUserById(userId) ?: throw IncorrectCredentialsException("no user for given bearer")
        return SimpleAuthenticationInfo(user, bearer, name)
    }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo =
        userService.getUserByPrincipal(principals.primaryPrincipal)?.let {
            SimpleAuthorizationInfo(setOf(if (it.isAdmin) MjRoles.ADMIN else MjRoles.STUDENT, MjRoles.USER))
        } ?: SimpleAuthorizationInfo()

}

class OAAuthTokenFilter : AuthenticatingFilter() {

    override fun onAccessDenied(request: ServletRequest, response: ServletResponse): Boolean {
        if (getBearerHeader(request) == null) return true
        return executeLogin(request, response)
    }

    override fun onLoginFailure(
        token: AuthenticationToken?,
        e: AuthenticationException,
        request: ServletRequest?,
        response: ServletResponse?
    ): Boolean {
        (response as HttpServletResponse).sendError(HttpResponseCodes.SC_UNAUTHORIZED, e.message)
        return super.onLoginFailure(token, e, request, response)
    }

    override fun createToken(request: ServletRequest, response: ServletResponse): AuthenticationToken {
        return BearerToken(getBearerHeader(request)!!)
    }

}

private fun getBearerHeader(request: ServletRequest): String? {
    val authorizationHeader = (request as HttpServletRequest).getHeader(HttpHeaders.AUTHORIZATION)
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        return null
    }
    return authorizationHeader.removePrefix("Bearer").trim()
}

private class BearerToken(val bearer: String) : AuthenticationToken {
    override fun getPrincipal(): Any = bearer
    override fun getCredentials(): Any = bearer
}

class ApiExceptionMapper : ExceptionMapper<UnauthorizedException> {

    @Context
    private lateinit var request: HttpServletRequest;
    override fun toResponse(cause: UnauthorizedException): Response =
        Response.status(Response.Status.UNAUTHORIZED)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(cause.message)
            .build()

}