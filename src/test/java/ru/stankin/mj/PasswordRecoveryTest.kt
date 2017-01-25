package ru.stankin.mj

import com.icegreen.greenmail.util.GreenMail
import io.kotlintest.matchers.be
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.support.DelegatingSubject
import org.apache.shiro.web.mgt.WebSecurityManager
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.PasswordRecoveryService
import ru.stankin.mj.model.user.User
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.rested.security.PasswordRecoveryRealm
import ru.stankin.mj.testutils.InWeldTest
import java.net.URL
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Created by nickl on 25.01.17.
 */
class PasswordRecoveryTest : InWeldTest() {
    init {
        test("Should sendRecovery mail and accept auth") {
            val userResolver = bean<UserResolver>()

            val student = Student("PasswordRecoveryTest1", "1","1","1")
            val email = "PasswordRecoveryTest1@example.com".toLowerCase()
            student.email = email


            userResolver.saveUser(student)

            val mail = jndiResourceInjectionServices.mail


            val pwr = bean<PasswordRecoveryService>()
            pwr.sendRecovery(student).get()

            val messages = mail.getReceivedMessagesForDomain(email).toList()

            messages.size shouldBe 1

            val query = URL("""http:\S+""".toRegex().find(messages.get(0).content as String)!!.value).query

            val token = query.substringAfter("code=")

            pwr.getUserIdByToken(token) shouldBe student.id

            val info = DelegatingSubject(bean<WebSecurityManager>()).apply {
                login(PasswordRecoveryRealm.Token(token))
            }


            val authenticatedUser = info.principal as User

            authenticatedUser shouldBe student

            info.checkRoles(MjRoles.STUDENT, MjRoles.PASSWORDRECOVERY)

        }


    }

}