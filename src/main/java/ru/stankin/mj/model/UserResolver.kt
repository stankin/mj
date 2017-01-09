package ru.stankin.mj.model

import io.buji.pac4j.subject.Pac4jPrincipal
import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.sql2o.Sql2o
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.User
import ru.stankin.mj.model.user.UserDAO
import javax.annotation.PostConstruct

import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by nickl on 16.02.15.
 */
@Singleton
open class UserResolver @Inject constructor(private val sql2o: Sql2o) : UserDAO {

    @Inject
    internal lateinit var storage: Storage


    @PostConstruct
    open fun initIfRequired() {

        //language=SQL
        sql2o.beginTransaction().use { connection ->

            val adminsCount = connection.createQuery("SELECT COUNT(id) FROM adminuser LIMIT 1;")
                    .executeScalar(Int::class.java)
            if(adminsCount == 0) {
                val userId = connection
                        .createQuery("INSERT INTO users (login) " + "VALUES (:login);", true)
                        .addParameter("login", "admin")
                        .executeUpdate().getKey<Int>(Int::class.java)

                connection.createQuery("INSERT INTO adminuser (id, password) " + "VALUES (:id, :password);")
                        .addParameter("id", userId)
                        .addParameter("password", "adminadmin")
                        .executeUpdate()

                connection.commit();
            }
        }

    }


    override fun getUserBy(username: String, password: String): User? {
        var result: User? = getUserBy(username)

        if (result?.password != password)
            result = null

        return result
    }

    override fun getUserBy(username: String): User? {
        var result: User? = storage.getStudentByCardId(username)
        if (result == null)
            result = getAdminUser(username)
        return result
    }

    open fun getAdminUser(username: String): AdminUser? {


        return sql2o.open().use{ connection ->
            connection
                    .createQuery("SELECT users.id as id, users.login as username, * FROM users INNER JOIN adminuser on users.id = adminuser.id WHERE login = :login")
                    .addParameter("login", username)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(AdminUser::class.java)

        }

    }


    override fun saveUser(user: User): Boolean {
        log.debug("saving user {}", user)
        if (user is Student)
            storage.saveStudent(user, null)
        else {
            saveAdmin(user as AdminUser)
        }
        return true
    }

    private fun saveAdmin(admin: AdminUser) {

        sql2o.beginTransaction().use { connection ->

            if (admin.id == 0L) {

                val userId = connection
                        .createQuery("INSERT INTO users (login, email) VALUES (:username, :email)", true)
                        .bind(admin)
                        .executeUpdate().getKey<Long>(Long::class.java)

                admin.id = userId!!

                connection.createQuery("INSERT INTO adminuser (id, password) VALUES (:id, :password)")
                        .bind(admin)
                        .executeUpdate()
            } else {
                connection
                        .createQuery("UPDATE users  SET email = :email WHERE id = :id")
                        .bind(admin)
                        .executeUpdate()

                connection.createQuery("UPDATE adminuser SET password = :password WHERE id = :id")
                        .bind(admin)
                        .executeUpdate()
            }

            connection.commit()

        }
    }


    override fun getUserByPrincipal(principal: Any): User? {
      return when (principal) {
            is String -> getUserBy(principal)
            else -> throw UnsupportedOperationException("principals of type " + principal.javaClass.name + " are not suported")
        }
    }

    companion object {

        private val log = LogManager.getLogger(UserResolver::class.java)
    }

}
