package ru.stankin.mj

import io.buji.pac4j.subject.Pac4jPrincipal
import io.buji.pac4j.token.Pac4jToken
import io.kotlintest.matchers.be
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.support.DelegatingSubject
import org.apache.shiro.web.mgt.WebSecurityManager
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.rested.security.YandexProfile
import ru.stankin.mj.testutils.InWeldTest

/**
 * Created by nickl on 11.01.17.
 */
class AuthTest : InWeldTest() {
    init {
        test("Should always be admin admin") {
            val userResolver = bean<UserResolver>()
            val admin = userResolver.getUserBy("admin", "adminadmin") as AdminUser
            checkNotNull(admin)
            bean<AuthenticationsStore>().updatePassword(admin.id, "1234")
            userResolver.getUserBy("admin", "adminadmin") shouldBe null
            checkNotNull(userResolver.getUserBy("admin", "1234") as AdminUser)
        }

        test("admin could login with shiro") {
            val sm = bean<WebSecurityManager>()

            shouldThrow<org.apache.shiro.authc.AuthenticationException> {
                DelegatingSubject(sm).login(UsernamePasswordToken("admin", "qwerty"))
            }

            val info = DelegatingSubject(sm).apply { login(UsernamePasswordToken("admin", "1234")) }


            info.principal shouldBe "admin"
        }

        test("login with pac4j") {
            val sm = bean<WebSecurityManager>()
            val info = DelegatingSubject(sm).apply {
                login(Pac4jToken(linkedMapOf("YandexKey" to YandexProfile("testYandex", mapOf())), false))
            }

            val pac4jPrincipal = info.principal as Pac4jPrincipal
            checkNotNull(pac4jPrincipal)
            pac4jPrincipal.profile.id shouldBe "testYandex"
        }


        test("register oath login with pac4j") {
            val sm = bean<WebSecurityManager>()
            val yandexProfile = YandexProfile("testYandex", mapOf())

            val adminUser = bean<UserResolver>().getUserBy("admin") as AdminUser

            bean<AuthenticationsStore>().assignProfileToUser(adminUser.id, yandexProfile)

            val info = DelegatingSubject(sm).apply {
                login(Pac4jToken(linkedMapOf("YandexKey" to yandexProfile), false))
            }

            info.principal shouldBe "admin"

            val pac4jPrincipal = info.principals.oneByType(Pac4jPrincipal::class.java)
            checkNotNull(pac4jPrincipal)
            pac4jPrincipal.profile.id shouldBe "testYandex"
        }

    }
}


