package ru.stankin.mj

import com.vaadin.cdi.CDIViewProvider
import io.kotlintest.matchers.be
import io.kotlintest.specs.FunSpec
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.support.DelegatingSubject
import org.apache.shiro.web.mgt.WebSecurityManager
import org.jboss.weld.bootstrap.api.CDI11Bootstrap
import org.jboss.weld.bootstrap.spi.Deployment
import org.jboss.weld.environment.se.Weld
import org.jboss.weld.environment.se.WeldContainer
import org.jboss.weld.injection.spi.ResourceInjectionServices
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices
import org.jboss.weld.manager.api.WeldManager
import org.jboss.weld.resources.spi.ResourceLoader
import org.sql2o.GenericDatasource
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.testutils.ContextStub
import ru.stankin.mj.testutils.InWeldTest
import ru.stankin.mj.testutils.JNDIResourceInjectionServices
import java.util.*
import javax.annotation.Resource
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Default
import javax.enterprise.inject.Produces
import javax.enterprise.inject.spi.InjectionPoint
import javax.inject.Singleton
import javax.naming.*

/**
 * Created by nickl on 11.01.17.
 */
class AuthTest : InWeldTest() {
    init {
        test("Should always be admin admin") {
            val userResolver = bean<UserResolver>()
            val admin = userResolver.getUserBy("admin", "adminadmin") as AdminUser
            checkNotNull(admin)
            admin.password = "1234"
            userResolver.saveUser(admin)
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


