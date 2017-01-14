package ru.stankin.mj.model

import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.credential.DefaultPasswordService
import org.apache.shiro.authc.credential.PasswordService
import org.apache.shiro.crypto.hash.DefaultHashService
import org.sql2o.Connection
import org.sql2o.Query
import org.sql2o.Sql2o
import ru.stankin.mj.utils.JSON
import ru.stankin.mj.utils.ThreadLocalTransaction
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AuthenticationsStore @Inject constructor(private val sql2o: Sql2o) {

    private val log = LogManager.getLogger(AuthenticationsStore::class.java)

    @Produces
    open fun getPasswordService(): PasswordService = passwordService


    companion object {

        val passwordService = DefaultPasswordService().apply {
            hashService = DefaultHashService().apply {
                hashAlgorithmName = "SHA-256"
                hashIterations = 300
                isGeneratePublicSalt = true
            }
        }
    }

    open fun updatePassword(userId: Int, password: String) {
        check(!password.isNullOrBlank(),  {->"password could not be null or blank"})

        fun Query.setParams(): Query = this
                .addParameter("id", userId)
                .addParameter("method", "password")
                .addParameter("value", JSON.asJson(mapOf("password" to passwordService.encryptPassword(password))))



        sql2o.beginTransaction(ThreadLocalTransaction.get()).use {
            connection ->

            val updated = connection.createQuery("UPDATE authentication set creation_date = now() AT TIME ZONE 'MSK', value = cast(:value AS JSON) WHERE user_id = :id AND method = :method")
                    .setParams()
                    .executeUpdate().result

            log.debug("updatePassword: for user ${userId} there are ${updated} passwords updated")

            if(updated == 0)
            connection.createQuery("INSERT INTO authentication (user_id, method, creation_date, value) VALUES (:id, :method, now() AT TIME ZONE 'MSK',cast(:value AS JSON))  ")
                    .setParams()
                    .executeUpdate()

            connection.commit()

        }

    }

    open fun acceptPassword(userId: Int, password: String): Boolean {

        val value = getAuthenicationValue(userId, "password")
        if (value == null)
            return false

        return passwordService.passwordsMatch(password, value["password"]!!)

    }

    private fun getAuthenicationValue(userId: Int, method: String): Map<String, String>? =
            sql2o.open(ThreadLocalTransaction.get()).use {
        connection ->
        connection.createQuery("SELECT cast(value AS TEXT) FROM authentication WHERE user_id =:id AND method =:method AND (authentication.expiration_date IS NULL OR authentication.expiration_date > now() AT TIME ZONE 'MSK') LIMIT 1")
                .addParameter("id", userId)
                .addParameter("method", method).executeScalar(String::class.java)?.let {
            JSON.read<Map<String, String>>(it)
        }
    }

    open fun getStoredPassword(id: Int): Any? = getAuthenicationValue(id, "password")?.let { it["password"]!! }

}