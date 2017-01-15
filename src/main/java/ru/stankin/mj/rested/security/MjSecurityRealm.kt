package ru.stankin.mj.rested.security

import io.buji.pac4j.subject.Pac4jPrincipal
import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.*
import org.apache.shiro.authc.credential.PasswordMatcher
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.realm.Realm
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.user.User
import ru.stankin.mj.model.user.UserDAO
import java.util.*


class MjSecurityRealm(private val userService: UserDAO, private val authService: AuthenticationsStore) : AuthorizingRealm() {



    init {
        this.credentialsMatcher = PasswordMatcher().apply {
            this.passwordService = this@MjSecurityRealm.authService.getPasswordService()
        }
    }

    override fun supports(token: AuthenticationToken?): Boolean {
        return token is UsernamePasswordToken
    }

    override fun doGetAuthorizationInfo(p0: PrincipalCollection): AuthorizationInfo {
        return userService.getUserByPrincipal(p0.primaryPrincipal)?.let {
            SimpleAuthorizationInfo(setOf(if (it.isAdmin) MjRoles.ADMIN else MjRoles.STUDENT, MjRoles.USER))
        } ?: SimpleAuthorizationInfo()
    }

    override fun doGetAuthenticationInfo(token: AuthenticationToken?): AuthenticationInfo? {

        return when(token) {
           is UsernamePasswordToken -> authenicateByUsernamePasswordToken(token)
            else -> throw UnsupportedOperationException("unsupported token type ${token?.javaClass}")
        }

    }

    private fun authenicateByUsernamePasswordToken(userPassToken: UsernamePasswordToken): SimpleAuthenticationInfo? {

        val user = userService.getUserBy(userPassToken.username)
        if (user != null) {
            return SimpleAuthenticationInfo(user, authService.getStoredPassword(user.id), name)
        } else
            throw IncorrectCredentialsException()
    }

    companion object {
       private val log = LogManager.getLogger(MjSecurityRealm::class.java)
    }

}

class MjOauthSecurityRealm(private val userService: UserDAO,
                           private val authService: AuthenticationsStore,
                           vararg realms: Realm) : AuthorizingRealm() {

    val log = LogManager.getLogger(MjOauthSecurityRealm::class.java)

    override fun supports(token: AuthenticationToken?): Boolean = realms.any { it.supports(token) }

    override fun doGetAuthenticationInfo(token: AuthenticationToken?): AuthenticationInfo? {
        val otherAuthInfo = realms.asSequence()
                .filter { it.supports(token) }
                .mapNotNull { realm -> realm.getAuthenticationInfo(token) }.firstOrNull()

        if (otherAuthInfo == null)
            return null;

        log.debug("otherAuthInfo = " + otherAuthInfo)

        val pac4jPrincipal = otherAuthInfo.principals.first() as Pac4jPrincipal

        val user = userService.getUserByPrincipal(pac4jPrincipal)

        val elems = ArrayList<Any>(2)
        if (user != null)
            elems.add(user)
        elems.add(pac4jPrincipal)
        val principals = SimplePrincipalCollection(elems, this.javaClass.simpleName)


        return SimpleAuthenticationInfo(principals, otherAuthInfo.credentials)
    }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo {

        if (principals.primaryPrincipal is Pac4jPrincipal)
            return SimpleAuthorizationInfo(setOf(MjRoles.UNBINDED_OAUTH))
        else
            return SimpleAuthorizationInfo()
    }

    private val realms = realms.asList()

}

object MjRoles {

    const val UNBINDED_OAUTH = "unbindedOauth"
    const val USER = "user"
    const val ADMIN = "admin"
    const val STUDENT = "student"

    public @JvmStatic fun getUser(): User? = SecurityUtils.getSubject().principals.oneByType(User::class.java)

}