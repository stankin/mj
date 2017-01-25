package ru.stankin.mj

import com.icegreen.greenmail.util.GreenMail
import io.kotlintest.matchers.be
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.PasswordRecoveryService
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

            mail.setUser(email, email, "secret-pwd");
            val messages = mail.getReceivedMessagesForDomain(email).toList()

            println("messages = " + messages.joinToString())
            messages.size shouldBe 1

            val query = URL("""http:\S+""".toRegex().find(messages.get(0).content as String)!!.value).query

            println("query = " + query)



//            val admin = userResolver.getUserBy("admin", "adminadmin") as AdminUser
//            checkNotNull(admin)
//            bean<AuthenticationsStore>().updatePassword(admin.id, "1234")
//            userResolver.getUserBy("admin", "adminadmin") shouldBe null
//            checkNotNull(userResolver.getUserBy("admin", "1234") as AdminUser)
        }


        test("testFromSample"){

            val greenMail = jndiResourceInjectionServices.mail

            val smtpSession = greenMail.getSmtp().createSession();

            val msg = MimeMessage(smtpSession);
            msg.setFrom(InternetAddress("foo@example.com"));
            msg.addRecipient(Message.RecipientType.TO,
                    InternetAddress("bar@example.com"));
            msg.setSubject("Email sent to GreenMail via plain JavaMail");
            msg.setText("Fetch me via IMAP");
            Transport.send(msg);

            // Create user, as connect verifies pwd
            greenMail.setUser("bar@example.com", "bar@example.com", "secret-pwd");

//            // Alternative 1: Create session and store or ...
//            val imapSession = greenMail.getImap().createSession();
//            val store = imapSession.getStore("imap");
//            store.connect("bar@example.com", "secret-pwd");
//            var inbox = store.getFolder("INBOX");
//            inbox.open(Folder.READ_ONLY);
//            var msgReceived = inbox.getMessage(1);
//            msg.getSubject() shouldBe  msgReceived.getSubject()
//
//
//            // Alternative 2: ... let GreenMail create and configure a store:
//            val imapStore = greenMail.getImap().createStore();
//            imapStore.connect("bar@example.com", "secret-pwd");
//            inbox = imapStore.getFolder("INBOX");
//            inbox.open(Folder.READ_ONLY);
//            msgReceived = inbox.getMessage(1);




            // Alternative 3: ... directly fetch sent message using GreenMail API
            greenMail.getReceivedMessagesForDomain("bar@example.com").size shouldBe 1
            val msgReceived = greenMail.getReceivedMessagesForDomain("bar@example.com")[0];
            println("msgReceived = " + msgReceived)

        }
    }

}