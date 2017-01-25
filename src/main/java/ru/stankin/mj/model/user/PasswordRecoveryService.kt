package ru.stankin.mj.model.user

import ru.stankin.mj.model.Student
import ru.stankin.mj.utils.requireProperty
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import javax.annotation.PreDestroy
import javax.annotation.Resource
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


/**
 * Created by nickl on 24.01.17.
 */
@Singleton
class PasswordRecoveryService {


    @Resource(lookup = "java:jboss/mail/Default")
    lateinit var mailSession: Session

    @Inject
    lateinit var properties: Properties

    private val senderThread = Executors.newSingleThreadExecutor()


    fun sendRecovery(user: User): CompletableFuture<Void> =  CompletableFuture.runAsync(Runnable { doSend(user) }, senderThread)


    private fun doSend(user: User) {
        val message = MimeMessage(mailSession)
        message.setFrom(InternetAddress(properties.requireProperty("serviceemail"), "Stankin MJ"))

        check(!user.email.isNullOrEmpty(), { "user email should not be empty" })

        message.addRecipient(Message.RecipientType.TO, InternetAddress(user.email))
        message.setSubject("Восстановление доступа в Модульный Журнал")
        message.setContent("Пройдите по ссылке http://uits-labs.ru/mj/recovery?code=1234", "text/plain; charset=UTF-8")
        Transport.send(message)
    }

    @PreDestroy
    fun close() {
        senderThread.shutdown()
    }


}