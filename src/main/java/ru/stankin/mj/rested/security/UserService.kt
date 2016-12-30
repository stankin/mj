package ru.stankin.mj.rested.security

import io.buji.pac4j.subject.Pac4jPrincipal
import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.*
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.realm.Realm
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import ru.stankin.mj.model.user.UserDAO
import javax.inject.Inject

class UserService {


    @Inject
    lateinit var userDao: UserDAO


    fun validUser(username: String, password: CharArray?): Boolean {
        return userDao.getUserBy(username, String(password!!)) != null;
    }

}

class MjSecurityRealm(private val userService: UserService, vararg realms: Realm) : AuthorizingRealm() {

    private val realms = realms.asList()


    override fun supports(token: AuthenticationToken?): Boolean {
        return super.supports(token) || realms.any { it.supports(token) }
    }

    override fun doGetAuthorizationInfo(p0: PrincipalCollection?): AuthorizationInfo {
        if(p0 == null)
            throw AuthorizationException("PrincipalCollection method argument cannot be null.")

        return SimpleAuthorizationInfo()
    }

    override fun doGetAuthenticationInfo(token: AuthenticationToken?): AuthenticationInfo? {

        return when(token) {
           is UsernamePasswordToken -> authenicateByUsernamePasswordToken(token)
           else -> {
               val otherAuthInfo = realms.asSequence()
                       .filter { it.supports(token) }
                       .mapNotNull { realm -> realm.getAuthenticationInfo(token) }.firstOrNull()

               if(otherAuthInfo == null)
                   return null;

               log.debug("otherAuthInfo = " + otherAuthInfo)
               val pac4jPrincipal = otherAuthInfo.principals.first() as Pac4jPrincipal
               SimpleAuthenticationInfo("admin", otherAuthInfo.credentials, otherAuthInfo.principals.realmNames.first() )
           }
        }

    }

    private fun authenicateByUsernamePasswordToken(userPassToken: UsernamePasswordToken): SimpleAuthenticationInfo? {

        if (userService.validUser(userPassToken.username, userPassToken.password)) {
            return SimpleAuthenticationInfo(userPassToken.username, userPassToken.password, name)
        } else
            throw IncorrectCredentialsException()
    }

    companion object {
       private val log = LogManager.getLogger(MjSecurityRealm::class.java)
    }

}