package ru.stankin.mj.rested.security

import org.apache.shiro.authc.*
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

class UserService {

}

class SecurityRealm(userService: UserService) : AuthorizingRealm() {
    override fun doGetAuthorizationInfo(p0: PrincipalCollection?): AuthorizationInfo {
        if(p0 == null)
            throw AuthorizationException("PrincipalCollection method argument cannot be null.")

        return SimpleAuthorizationInfo()
    }

    override fun doGetAuthenticationInfo(token: AuthenticationToken?): AuthenticationInfo? {

        val userPassToken = token as? UsernamePasswordToken ?: return null;

        if(userPassToken.username == "nickl"){
            return SimpleAuthenticationInfo(userPassToken.username, userPassToken.password, name)
        }
        else
            throw IncorrectCredentialsException();

    }

}