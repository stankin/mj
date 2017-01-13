package ru.stankin.mj.rested.security

import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.credential.PasswordService
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
import org.pac4j.oauth.client.Google2Client
import org.pac4j.oauth.client.VkClient
import ru.stankin.mj.model.user.UserDAO
import java.util.*


/**
 * Created by nickl on 20.11.16.
 */
class ShiroConfiguration {

    private val logger = LogManager.getLogger(ShiroConfiguration::class.java)

    @Inject
    lateinit var userService: UserDAO

    @Inject
    lateinit var passwordService: PasswordService

    @Inject
    lateinit var properties: Properties

    @Produces
    fun getSecurityManager(): WebSecurityManager = DefaultWebSecurityManager(mutableListOf<Realm>(
            MjSecurityRealm(userService, passwordService, io.buji.pac4j.realm.Pac4jRealm())
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
                    Google2Client(
                            properties.getProperty("oauth.google.clientid")!!,
                            properties.getProperty("oauth.google.secret")!!
                    ).apply {
                       callbackUrl = properties.getProperty("oauth.callbackurl")
                    },
                    VkClient(properties.getProperty("oauth.vk.clientid")!!,
                            properties.getProperty("oauth.vk.secret")!!)
                            .apply {
                        callbackUrl = properties.getProperty("oauth.callbackurl")
                            },
                    YandexClient(properties.getProperty("oauth.yandex.clientid")!!,
                            properties.getProperty("oauth.yandex.secret")!!).apply {
                        callbackUrl = properties.getProperty("oauth.callbackurl")
                    }

            )
        }

        val fcMan = DefaultFilterChainManager().apply {
            addFilter("basic", org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter())
            addFilter("callbackFilter", io.buji.pac4j.filter.CallbackFilter().apply {
                config = pacConfig
                defaultUrl = "/mj"
            })
            addFilter("forceLoginFilter", ForceLoginFilter().apply {
                config = pacConfig
            })
            createChain("/webapi/api3/**", "basic");
            createChain("/callback", "callbackFilter");
            createChain("/forceLogin", "forceLoginFilter");
        }


        return PathMatchingFilterChainResolver().apply { filterChainManager = fcMan }
    }


}