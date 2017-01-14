package ru.stankin.mj

import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.support.DelegatingSubject
import org.apache.shiro.web.mgt.WebSecurityManager
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
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

            shouldThrow<org.apache.shiro.authc.IncorrectCredentialsException> {
                DelegatingSubject(sm).login(UsernamePasswordToken("admin", "qwerty"))
            }

            val info = DelegatingSubject(sm).apply { login(UsernamePasswordToken("admin", "1234")) }

            info.principal shouldBe "admin"
        }
    }
}


