package ru.stankin.mj.model.user

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.sql2o.Sql2o
import ru.stankin.mj.UserActionException
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.Subject
import ru.stankin.mj.utils.JSON
import ru.stankin.mj.utils.ThreadLocalTransaction
import ru.stankin.mj.utils.requireProperty
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
class PasswordRecoveryService @Inject constructor(private val sql2o: Sql2o) {

    private val log = LogManager.getLogger(PasswordRecoveryService::class.java)

    @Resource(lookup = "java:jboss/mail/Default")
    lateinit var mailSession: Session

    @Inject
    lateinit var properties: Properties

    private val senderThread = Executors.newSingleThreadExecutor()


    private val atemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(600, TimeUnit.SECONDS)
            .build<String, AtomicInteger>(CacheLoader.from { id -> AtomicInteger(0) })

    @Throws(UserActionException::class)
    fun sendRecovery(user: User): CompletableFuture<Void> {

        if (user.email.isNullOrEmpty())
            throw UserActionException("Адрес электронной почты не установлен для пользователя. Обратитесть в деканат для восстановления пароля.")

        if(atemptsCache.get(user.username).incrementAndGet() > 3)
            throw UserActionException("Ссылка для восстановления уже была отправлена. Вы сможете повторить попытку через некоторое время")


        log.debug("ordering password recovering for user {}", user)
        val future = CompletableFuture.runAsync(Runnable { doSend(user) }, senderThread)
        future.whenCompleteAsync { r, throwable ->
            if(r != null)
                log.debug("email for password recovery was sent user {}", user)
            if(throwable != null)
                log.warn("error recovering password for user user {}", user, throwable)

        }
        return future
    }



    private fun doSend(user: User) {
        val message = MimeMessage(mailSession)
        message.setFrom(InternetAddress(properties.requireProperty("service.email"), "Stankin MJ"))

        val token = createToken(user)

        message.addRecipient(Message.RecipientType.TO, InternetAddress(user.email))
        message.setSubject("Восстановление доступа в Модульный Журнал")
        message.setContent("Если вы забыли пароль для входа в Модульный Журнал, то, пожалуйста, " +
                "пройдите по ссылке ${properties.requireProperty("service.recoveryurl")}?code=${token} и установите новый пароль." +
                " Ссылка действительна в течении 20 минут.", "text/plain; charset=UTF-8")
        Transport.send(message)
    }


    private fun createToken(user: User): String {
        val token = UUID.randomUUID().toString()
        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("INSERT INTO passwordrecovery (user_id,order_time, token) VALUES (:id, now() AT TIME ZONE 'MSK',:token)  ")
                    .addParameter("id", user.id)
                    .addParameter("token", token)
                    .executeUpdate()
            connection.commit()
        }
        return token
    }

    fun getUserIdByToken(token: String): Int? {

        val leastTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow")).minusMinutes(20)

        val user = sql2o.open(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("SELECT user_id FROM passwordrecovery WHERE token = :token AND order_time > :date LIMIT 1")
                    .addParameter("date", Timestamp.from(leastTime.toInstant()))
                    .addParameter("token", token)
                    .executeScalar<Int>(Int::class.java)

        }

        log.debug("user id for token {} is {}", token, user)

        return user
    }

    @PreDestroy
    fun close() {
        senderThread.shutdown()
    }


}