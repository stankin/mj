package ru.stankin.mj.model

import io.buji.pac4j.subject.Pac4jPrincipal
import org.apache.logging.log4j.LogManager
import org.sql2o.Sql2o
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.User
import ru.stankin.mj.utils.ThreadLocalTransaction
import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by nickl on 16.02.15.
 */
@Singleton
class UserResolver @Inject constructor(private val sql2o: Sql2o) {

    @Inject
    private lateinit var storage: StudentsStorage

    @Inject
    private lateinit var auth: AuthenticationsStore


    @PostConstruct
    fun initIfRequired() {

        ThreadLocalTransaction.within(sql2o) { ->

            sql2o.beginTransaction(ThreadLocalTransaction.get()!!).use { connection ->

                val adminsCount = connection.createQuery("SELECT COUNT(id) FROM adminuser LIMIT 1;")
                        .executeScalar(Int::class.java)
                if (adminsCount == 0) {
                    val userId = connection
                            .createQuery("INSERT INTO users (login) VALUES (:login);", true)
                            .addParameter("login", "admin")
                            .executeUpdate().getKey<Int>(Int::class.java)

                    connection.createQuery("INSERT INTO adminuser (id) VALUES (:id);")
                            .addParameter("id", userId)
                            .executeUpdate()

                    auth.updatePassword(userId, "adminadmin")

                    connection.commit();
                }
            }

        }

    }


    fun getUserBy(username: String, password: String): User? {
        val result: User? = getUserBy(username)
        if (result == null)
            return null

        if (!auth.acceptPassword(result.id, password))
            return null

        return result
    }

    fun getUserBy(username: String): User? {
        var result: User? = storage.getStudentByCardId(username)
        if (result == null)
            result = getAdminUser(username)
        return result
    }

    private fun getAdminUser(username: String): AdminUser? {

        return sql2o.open().use{ connection ->
            connection
                    .createQuery("SELECT users.id as id, users.login as username, * FROM users INNER JOIN adminuser on users.id = adminuser.id WHERE login = :login")
                    .addParameter("login", username)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(AdminUser::class.java)

        }

    }

    private fun getAdminUser(id: Int): AdminUser? {

        return sql2o.open().use{ connection ->
            connection
                    .createQuery("SELECT users.id as id, users.login as username, * FROM users INNER JOIN adminuser on users.id = adminuser.id WHERE users.id = :id")
                    .addParameter("id", id)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(AdminUser::class.java)

        }

    }


    fun saveUser(user: User): Boolean {
        log.debug("saving user {}", user)
        if (user is Student)
            storage.saveStudent(user, null)
        else {
            saveAdmin(user as AdminUser)
        }
        return true
    }

    fun saveUserAndPassword(user: User, password: String): Boolean {
        ThreadLocalTransaction.joinOrNew(sql2o) { ->
            saveUser(user)
            if (!password.isNullOrBlank())
                auth.updatePassword(user.id, password)
        }
        return true
    }

    private fun saveAdmin(admin: AdminUser) {

        sql2o.beginTransaction().use { connection ->

            if(admin.email.isNullOrBlank())
                admin.email = null

            if (admin.id == 0) {

                val userId = connection
                        .createQuery("INSERT INTO users (login, email) VALUES (:username, :email)", true)
                        .bind(admin)
                        .executeUpdate().getKey<Int>(Int::class.java)

                admin.id = userId!!

                connection.createQuery("INSERT INTO adminuser (id) VALUES (:id)")
                        .bind(admin)
                        .executeUpdate()
            } else {
                connection
                        .createQuery("UPDATE users  SET email = :email WHERE id = :id")
                        .bind(admin)
                        .executeUpdate()

//                connection.createQuery("UPDATE adminuser SET password = :password WHERE id = :id")
//                        .bind(admin)
//                        .executeUpdate()
            }

            connection.commit()

        }
    }


    fun getUserByPrincipal(principal: Any): User? {
        return when (principal) {
            is String -> getUserBy(principal)
            is User -> principal
            is Pac4jPrincipal -> auth.findUserByOauth(principal.profile)?.let { getUserById(it) }
            else -> throw UnsupportedOperationException("principals of type " + principal.javaClass.name + " are not suported")
        }
    }

    fun getUserById(id: Int): User? = storage.getStudentById(id, null) ?: getAdminUser(id)

    companion object {

        private val log = LogManager.getLogger(UserResolver::class.java)
    }

}
