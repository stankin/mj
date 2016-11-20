package ru.stankin.mj.rested.security

import org.apache.shiro.web.filter.mgt.FilterChainResolver
import org.apache.shiro.web.mgt.DefaultWebSecurityManager
import org.apache.shiro.web.mgt.WebSecurityManager
import javax.enterprise.inject.Produces
import javax.inject.Inject
import org.apache.shiro.web.filter.mgt.FilterChainManager
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver


/**
 * Created by nickl on 20.11.16.
 */
class ShiroConfiguration {

    @Inject
    lateinit var userService: UserService

    @Produces
    fun getSecurityManager(): WebSecurityManager = DefaultWebSecurityManager(SecurityRealm(userService))


    @Produces
    fun getFilterChainResolver(): FilterChainResolver {

        val fcMan = DefaultFilterChainManager().apply {
            addFilter("basic", org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter())
            createChain("/**", "basic");
        }


        return PathMatchingFilterChainResolver().apply { filterChainManager = fcMan }
    }


}