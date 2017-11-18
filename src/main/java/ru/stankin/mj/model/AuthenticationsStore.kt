package ru.stankin.mj.model

import org.apache.logging.log4j.LogManager
import org.apache.shiro.authc.credential.DefaultPasswordService
import org.apache.shiro.authc.credential.PasswordService
import org.apache.shiro.crypto.hash.DefaultHashService
import org.pac4j.core.profile.CommonProfile
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
        check(!password.isNullOrBlank(), { "password could not be null or blank" })

        fun Query.setParams(): Query = this
                .addParameter("id", userId)
                .addParameter("method", "password")
                .addParameter("value", JSON.asJson(mapOf("password" to passwordService.encryptPassword(password))))

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use {
            connection ->

            val updated = connection.createQuery("UPDATE authentication SET creation_date = now() AT TIME ZONE 'MSK', value = cast(:value AS JSONB) WHERE user_id = :id AND method = :method")
                    .setParams()
                    .executeUpdate().result

            log.debug("updatePassword: for user ${userId} there are ${updated} passwords updated")

            if (updated == 0)
                connection.createQuery("INSERT INTO authentication (user_id, method, creation_date, value) VALUES (:id, :method, now() AT TIME ZONE 'MSK',cast(:value AS JSON))  ")
                        .setParams()
                        .executeUpdate()

            connection.commit()

        }

    }

    fun assignProfileToUser(userId: Int, profile: CommonProfile) {

        val profileValue = mapOf(
                "provider" to profile.clientName!!,
                "id" to profile.id!!,
                "attributes" to profile.attributes
        )

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("INSERT INTO authentication (user_id, method, creation_date, value) VALUES (:id, :method, now() AT TIME ZONE 'MSK',cast(:value AS JSONB))  ")
                    .addParameter("id", userId)
                    .addParameter("method", "oauth")
                    .addParameter("value", JSON.asJson(profileValue))
                    .executeUpdate()

            connection.commit()
        }
    }

    fun findUserByOauth(profile: CommonProfile): Int? {

        // SELECT * FROM authentication WHERE method = 'oauth' and authentication.value @> '{"id":"testYandex" , "provider": "YandexProfile"}'

        return sql2o.open(ThreadLocalTransaction.get()).use {
            connection ->
            connection.createQuery("SELECT user_id FROM authentication WHERE  method =:method AND (authentication.expiration_date IS NULL OR authentication.expiration_date > now() AT TIME ZONE 'MSK') AND authentication.value @> cast(:json AS JSONB) LIMIT 1")
                    .addParameter("method", "oauth")
                    .addParameter("json", "{\"id\": \"${profile.id!!}\" , \"provider\": \"${profile.clientName!!}\"}")
                    .executeScalar(Int::class.java)

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

    fun dropExternalAuths(userId: Int) {
        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("DELETE FROM authentication WHERE user_id = :id AND method = :method")
                    .addParameter("id", userId)
                    .addParameter("method", "oauth")
                    .executeUpdate()

            connection.commit()
        }
    }

    fun markUsed(userId: Int, profile: CommonProfile) {

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use { connection ->
            connection.createQuery("UPDATE authentication SET lastused_date = now() AT TIME ZONE 'MSK' " +
                    "WHERE user_id = :id AND authentication.value @> cast(:json AS JSONB)")
                    .addParameter("id", userId)
                    .addParameter("json", "{\"id\": \"${profile.id!!}\" , \"provider\": \"${profile.clientName!!}\"}")
                    .executeUpdate()
            connection.commit()
        }
    }

    fun markUsedPassword(userId: Int) {

        sql2o.beginTransaction(ThreadLocalTransaction.get()).use {
            connection ->

            connection.createQuery("UPDATE authentication SET lastused_date = now() AT TIME ZONE 'MSK' WHERE user_id = :id AND method = :method")
                    .addParameter("id", userId)
                    .addParameter("method", "password")
                    .executeUpdate()

            connection.commit()
        }
    }

}