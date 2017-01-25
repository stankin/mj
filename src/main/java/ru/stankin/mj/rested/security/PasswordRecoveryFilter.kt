package ru.stankin.mj.rested.security

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by nickl on 25.01.17.
 */
class PasswordRecoveryFilter: Filter {
    override fun destroy() {

    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

        (request as HttpServletRequest).getParameter("code")?.let {
            try {
                SecurityUtils.getSubject().login(PasswordRecoveryRealm.Token(it))
            } catch (e: AuthenticationException){
            }
        }

        (response as HttpServletResponse).sendRedirect(request.contextPath)

    }

    override fun init(filterConfig: FilterConfig?) {

    }
}