package ru.stankin.mj.oauthprovider

import com.google.common.cache.CacheBuilder
import org.apache.logging.log4j.LogManager
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction.tlTransaction
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by nickl on 14.03.17.
 */
@Singleton
class OAuthProvider @Inject constructor(private val sql2o: Sql2o) {

    private val log = LogManager.getLogger(OAuthProvider::class.java)


    fun registerConsumer(serviceName: String, email: String, redirects: List<String>): ConsumerAuthentication {

        val clientId = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()

        sql2o.tlTransaction { connection ->
            connection.createQuery("INSERT INTO OAuthConsumer (email, service_name, client_id, secret, redirects) VALUES (:email, :name, :client_id, :secret, ARRAY[:redirects])")
                    .addParameter("email", email)
                    .addParameter("name", serviceName)
                    .addParameter("client_id", clientId)
                    .addParameter("secret", secret)
                    .addParameter("redirects", redirects.toTypedArray())
                    .executeUpdate()
                    .commit()
        }

        return ConsumerAuthentication(clientId, secret)
    }

    fun addUserPermission(clientId: String, userId: Long): String {

        val token = UUID.randomUUID().toString()

        sql2o.tlTransaction { connection ->
            connection.createQuery("INSERT INTO OAuthConsumerPermissions (consumer_id, user_id, token) " +
                    "VALUES ((SELECT consumer_id from OAuthConsumer where client_id = :client_id), :userId, :token)" +
                    "ON CONFLICT(consumer_id, user_id) DO UPDATE SET token=:token, creation_date=DEFAULT")
                    .addParameter("client_id", clientId)
                    .addParameter("userId", userId)
                    .addParameter("token", token)
                    .executeUpdate()
                    .commit()
        }

        return token
    }

    fun removeUserPermission(clientId: String, userId: Long) {
        sql2o.tlTransaction { connection ->
            connection.createQuery("DELETE FROM OAuthConsumerPermissions WHERE consumer_id = (SELECT consumer_id FROM OAuthConsumer WHERE client_id = :client_id) AND user_id = :userId")
                    .addParameter("client_id", clientId)
                    .addParameter("userId", userId)
                    .executeUpdate()
                    .commit()
        }
    }

    fun getSavedToken(clientId: String, userId: Long): String? {
        sql2o.open().use { connection ->
            val permission = connection.createQuery("SELECT token FROM OAuthConsumerPermissions WHERE consumer_id = (SELECT consumer_id FROM OAuthConsumer WHERE client_id = :client_id) AND user_id = :userId")
                    .addParameter("client_id", clientId)
                    .addParameter("userId", userId)
                    .executeScalar(String::class.java)

            log.debug("getting saved permission for {} {} -> {}", clientId, userId, permission)
            return permission
        }
    }

    fun getUserIdByToken(token: String): Int? {
        sql2o.open().use { connection ->
            val userId = connection.createQuery("SELECT user_id FROM OAuthConsumerPermissions WHERE token = :token")
                    .addParameter("token", token)
                    .executeScalar(Int::class.java)

            return userId
        }
    }

    private val temporaryCodes = CacheBuilder.newBuilder()
            .expireAfterWrite(600, TimeUnit.SECONDS)
            .build<Long, ResolvedUser>()


    fun makeUserTemporaryCode(clientId: String, userId: Long): Long? {
        val token = getSavedToken(clientId, userId) ?: return null

        val code = Math.abs(UUID.randomUUID().leastSignificantBits)
        val resolvedUser = ResolvedUser(clientId, userId, token)
        temporaryCodes.put(code, resolvedUser)
        return code
    }

    fun resolveByTemporaryCode(code: Long): ResolvedUser? = temporaryCodes.getIfPresent(code)
    fun getConsumer(clientId: String, secret: String): ConsumerInfo? {
        sql2o.open().use { connection ->
            return connection.createQuery("SELECT service_name AS serviceName, email, redirects FROM OAuthConsumer WHERE client_id = :clientId AND secret = :secret")
                    .addParameter("clientId", clientId)
                    .addParameter("secret", secret)
                    .executeAndFetchFirst(ConsumerInfo::class.java)
        }
    }

    fun getConsumer(clientId: String): ConsumerInfo? {
        sql2o.open().use { connection ->
            return connection.createQuery("SELECT service_name AS serviceName, email, redirects FROM OAuthConsumer WHERE client_id = :clientId")
                    .addParameter("clientId", clientId)
                    .executeAndFetchFirst(ConsumerInfo::class.java)
        }
    }


}

data class ResolvedUser(val clientId: String, val userId: Long, val token: String)

data class ConsumerInfo(val serviceName: String, val email: String, val redirects: List<String>)

data class ConsumerAuthentication(val token: String, val secret: String)