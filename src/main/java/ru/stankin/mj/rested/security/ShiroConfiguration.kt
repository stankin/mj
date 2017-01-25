package ru.stankin.mj.rested.security

import io.buji.pac4j.subject.Pac4jPrincipal
import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.AuthenticationListener
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.credential.PasswordService
import org.apache.shiro.authc.pam.ModularRealmAuthenticator
import org.apache.shiro.mgt.AbstractRememberMeManager
import org.apache.shiro.mgt.DefaultSubjectDAO
import org.apache.shiro.realm.Realm
import org.apache.shiro.session.Session
import org.apache.shiro.session.SessionListener
import org.apache.shiro.session.mgt.eis.MemorySessionDAO
import org.apache.shiro.subject.PrincipalCollection
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
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.PasswordRecoveryService
import ru.stankin.mj.model.user.User
import ru.stankin.mj.utils.requireProperty
import java.util.*


/**
 * Created by nickl on 20.11.16.
 */
class ShiroConfiguration {

    private val logger = LogManager.getLogger(ShiroConfiguration::class.java)

    @Inject
    lateinit var userService: UserResolver

    @Inject
    lateinit var authenticationsStore: AuthenticationsStore

    @Inject
    lateinit var pwr: PasswordRecoveryService

    @Inject
    lateinit var properties: Properties

    @Produces
    fun getSecurityManager(): WebSecurityManager = DefaultWebSecurityManager(mutableListOf<Realm>(
            MjSecurityRealm(userService, authenticationsStore),
            MjOauthSecurityRealm(userService, authenticationsStore, io.buji.pac4j.realm.Pac4jRealm()),
            PasswordRecoveryRealm(userService, pwr)
    )
    ).apply {
        authorizer
        (authenticator as ModularRealmAuthenticator).apply {
            this.authenticationListeners.add(succesfulAuthenticationsLogger)
        }
        subjectFactory = io.buji.pac4j.subject.Pac4jSubjectFactory()
        sessionManager = ServletContainerSessionManager().apply {
            val cipher = System.getenv("SHIRO_CIPHER_KEY")
            if (cipher != null)
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
                            properties.requireProperty("oauth.google.clientid"),
                            properties.requireProperty("oauth.google.secret")
                    ).apply {
                       callbackUrl = properties.getProperty("oauth.callbackurl")
                    },
                    VkClient(properties.requireProperty("oauth.vk.clientid"),
                            properties.requireProperty("oauth.vk.secret"))
                            .apply {
                        callbackUrl = properties.getProperty("oauth.callbackurl")
                            },
                    YandexClient(properties.requireProperty("oauth.yandex.clientid"),
                            properties.requireProperty("oauth.yandex.secret")).apply {
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
            addFilter("passwordRecovery", PasswordRecoveryFilter())
            createChain("/webapi/api3/**", "basic")
            createChain("/callback", "callbackFilter")
            createChain("/forceLogin", "forceLoginFilter")
            createChain("/recovery", "passwordRecovery")
        }


        return PathMatchingFilterChainResolver().apply { filterChainManager = fcMan }
    }

    private val succesfulAuthenticationsLogger = object : AuthenticationListener {
        override fun onSuccess(token: AuthenticationToken, info: AuthenticationInfo) {
            logger.debug("authenticated token {} info {}", token, info)
            try {
                val user = info.principals.oneByType(User::class.java)
                if (user != null) {
                    val pac4jprincipals = info.principals.byType(Pac4jPrincipal::class.java)
                    if (!pac4jprincipals.isEmpty())
                        for (principal in pac4jprincipals) {
                            authenticationsStore.markUsed(user.id, principal.profile)
                        }
                    else
                        authenticationsStore.markUsedPassword(user.id)
                }
            } catch (e: Exception) {
                logger.warn("succesfulAuthenticationsLogger exception:", e)
            }

        }

        override fun onFailure(token: AuthenticationToken?, ae: AuthenticationException?) {}

        override fun onLogout(principals: PrincipalCollection?) {}
    }

}

