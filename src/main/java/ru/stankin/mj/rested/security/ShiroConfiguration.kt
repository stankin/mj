package ru.stankin.mj.rested.security

import org.apache.logging.log4j.LogManager
import org.apache.shiro.mgt.AbstractRememberMeManager
import org.apache.shiro.mgt.DefaultSubjectDAO
import org.apache.shiro.realm.Realm
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
import org.pac4j.core.client.Clients


/**
 * Created by nickl on 20.11.16.
 */
class ShiroConfiguration {

    private val logger = LogManager.getLogger(ShiroConfiguration::class.java)

    @Inject
    lateinit var userService: UserService

    @Produces
    fun getSecurityManager(): WebSecurityManager = DefaultWebSecurityManager(mutableListOf<Realm>(
            MjSecurityRealm(userService, io.buji.pac4j.realm.Pac4jRealm())
    )
    ).apply {
        subjectFactory = io.buji.pac4j.subject.Pac4jSubjectFactory()
        sessionManager = ServletContainerSessionManager().apply {
            val cipher = System.getenv("SHIRO_CIPHER_KEY")
            if(cipher != null)
                (rememberMeManager as AbstractRememberMeManager).cipherKey = cipher.toByteArray()
            subjectDAO = DefaultSubjectDAO().apply {
                sessionStorageEvaluator = DefaultWebSessionStorageEvaluator().apply {
                    isSessionStorageEnabled = false
                }
            }
        }

    }


    @Produces
    fun getFilterChainResolver(): FilterChainResolver {

        val pacConfig = org.pac4j.core.config.Config().apply {
            clients = Clients(
                    org.pac4j.oauth.client.FacebookClient(
                            "145278422258960",
                            "be21409ba8f39b5dae2a7de525484da8"
                    ).apply {
                        callbackUrl = "http://localhost:8080/mj/callback"
                    }
            )
        }

        val fcMan = DefaultFilterChainManager().apply {
            addFilter("basic", org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter())
//            addFilter("facebook",io.buji.pac4j.filter.SecurityFilter().apply {
//                config = pacConfig
//                clients = "FacebookClient"
//            })
            addFilter("callbackFilter", io.buji.pac4j.filter.CallbackFilter().apply {
                config = pacConfig
                defaultUrl = "/mj"
            })
            addFilter("forceLoginFilter", ForceLoginFilter().apply {
                config = pacConfig
            })
            createChain("/webapi/api3/**", "basic");
//            createChain("/facebook/**", "facebook");
            createChain("/callback", "callbackFilter");
            createChain("/forceLogin", "forceLoginFilter");
        }


        return PathMatchingFilterChainResolver().apply { filterChainManager = fcMan }
    }


}