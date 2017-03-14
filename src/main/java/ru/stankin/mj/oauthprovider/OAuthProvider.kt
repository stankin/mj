package ru.stankin.mj.oauthprovider

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.apache.logging.log4j.LogManager
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction
import ru.stankin.mj.utils.ThreadLocalTransaction.joinOrNew
import ru.stankin.mj.utils.ThreadLocalTransaction.tlTransaction
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import org.apache.oltu.oauth2.`as`.issuer.MD5Generator
import org.apache.oltu.oauth2.`as`.issuer.OAuthIssuerImpl
import org.apache.oltu.oauth2.`as`.issuer.OAuthIssuer


/**
 * Created by nickl on 14.03.17.
 */
@Singleton
class OAuthProvider @Inject constructor(private val sql2o: Sql2o) {

    private val log = LogManager.getLogger(OAuthProvider::class.java)

    var oauthIssuerImpl: OAuthIssuer = OAuthIssuerImpl(MD5Generator())

    fun registerConsumer(serviceName: String, email: String): ConsumerAuthentication {

        val clientId = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()

        sql2o.tlTransaction { connection ->
            connection.createQuery("INSERT INTO OAuthConsumer (email, service_name, client_id, secret) VALUES (:email, :name, :client_id, :secret)")
                    .addParameter("email", email)
                    .addParameter("name", serviceName)
                    .addParameter("client_id", clientId)
                    .addParameter("secret", secret)
                    .executeUpdate()
                    .commit()
        }

        return ConsumerAuthentication(clientId, secret)
    }

    fun addUserPermission(serviceName: String, userId: Long) {

        val token = UUID.randomUUID().toString()

        sql2o.tlTransaction { connection ->
            connection.createQuery("INSERT INTO OAuthConsumerPermissions (consumer_id, user_id, token) " +
                    "VALUES ((SELECT consumer_id from OAuthConsumer where service_name = :service_name), :userId, :token)" +
                    "ON CONFLICT(consumer_id, user_id) DO UPDATE SET token=:token, creation_date=DEFAULT")
                    .addParameter("service_name", serviceName)
                    .addParameter("userId", userId)
                    .addParameter("token", token)
                    .executeUpdate()
                    .commit()
        }
    }

    fun getSavedToken(serviceName: String, userId: Long): String? {


        sql2o.open().use { connection ->
            val permission = connection.createQuery("SELECT token FROM OAuthConsumerPermissions WHERE consumer_id = (SELECT consumer_id FROM OAuthConsumer WHERE service_name = :service_name) AND user_id = :userId")
                    .addParameter("service_name", serviceName)
                    .addParameter("userId", userId)
                    .executeScalar(String::class.java)

            log.debug("getting saved permission for {} {} -> {}", serviceName, userId, permission)
            return permission
        }


    }

    private val temporaryCodes = CacheBuilder.newBuilder()
            .expireAfterWrite(600, TimeUnit.SECONDS)
            .build<Long, ResolvedUser>()


    fun makeUserTemporaryCode(serviceName: String, userId: Long): Long? {
        val token = getSavedToken(serviceName, userId) ?: return null

        val code = Math.abs(UUID.randomUUID().leastSignificantBits)
        val resolvedUser = ResolvedUser(serviceName, userId, token)
        temporaryCodes.put(code, resolvedUser)
        return code
    }

    fun resolveByTemporaryCode(code: Long): ResolvedUser? = temporaryCodes.getIfPresent(code)
    fun getConsumer(clientId: String, secret: String): ConsumerInfo? {
        sql2o.open().use { connection ->
            return connection.createQuery("SELECT service_name AS serviceName, email FROM OAuthConsumer WHERE client_id = :clientId AND secret = :secret")
                    .addParameter("clientId", clientId)
                    .addParameter("secret", secret)
                    .executeAndFetchFirst(ConsumerInfo::class.java)
        }
    }


}

data class ResolvedUser(val serviceName: String, val userId: Long, val token: String)

data class ConsumerInfo(val serviceName: String, val email: String)

data class ConsumerAuthentication(val token: String, val secret: String)