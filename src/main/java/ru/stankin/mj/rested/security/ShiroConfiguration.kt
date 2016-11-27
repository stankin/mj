package ru.stankin.mj.rested.security

import org.apache.logging.log4j.LogManager
import org.apache.shiro.mgt.DefaultSubjectDAO
import org.apache.shiro.session.Session
import org.apache.shiro.session.SessionListener
import org.apache.shiro.session.mgt.eis.MemorySessionDAO
import org.apache.shiro.web.filter.mgt.FilterChainResolver
import org.apache.shiro.web.mgt.DefaultWebSecurityManager
import org.apache.shiro.web.mgt.WebSecurityManager
import javax.enterprise.inject.Produces
import javax.inject.Inject
import org.apache.shiro.web.filter.mgt.FilterChainManager
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver
import org.apache.shiro.web.mgt.DefaultWebSessionStorageEvaluator
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager


/**
 * Created by nickl on 20.11.16.
 */
class ShiroConfiguration {

    private val logger = LogManager.getLogger(ShiroConfiguration::class.java)

    @Inject
    lateinit var userService: UserService

    @Produces
    fun getSecurityManager(): WebSecurityManager = DefaultWebSecurityManager(SecurityRealm(userService)).apply {
        sessionManager = ServletContainerSessionManager().apply {
//            sessionListeners.add(object : SessionListener {
//                override fun onExpiration(p0: Session?) {
//                    logger.info("onExpiration $p0")
//                }
//
//                override fun onStart(p0: Session?) {
//                    logger.info("onStart $p0")
//                }
//
//                override fun onStop(p0: Session?) {
//                    logger.info("onStop $p0")
//                }
//            })
            subjectDAO = DefaultSubjectDAO().apply {
                sessionStorageEvaluator = DefaultWebSessionStorageEvaluator().apply {
                    isSessionStorageEnabled = false
                }
            }
        }

    }


    @Produces
    fun getFilterChainResolver(): FilterChainResolver {

        val fcMan = DefaultFilterChainManager().apply {
            addFilter("basic", org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter())
            createChain("/webapi/**", "basic");
        }


        return PathMatchingFilterChainResolver().apply { filterChainManager = fcMan }
    }


}