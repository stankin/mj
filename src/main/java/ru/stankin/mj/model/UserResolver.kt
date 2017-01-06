package ru.stankin.mj.model

import io.buji.pac4j.subject.Pac4jPrincipal
import kotlinx.support.jdk7.use
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.sql2o.Sql2o
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.model.user.User
import ru.stankin.mj.model.user.UserDAO

import javax.inject.Inject
import javax.inject.Singleton
import javax.persistence.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root
import javax.transaction.Transactional

/**
 * Created by nickl on 16.02.15.
 */
@Singleton
open class UserResolver @Inject constructor(private val sql2o: Sql2o) : UserDAO {

    @Inject
    internal lateinit var storage: Storage

    @PersistenceContext
    private lateinit var em: EntityManager

    @Volatile private var initRequred = true
    //TODO: Maybe @javax.annotation.PostConstruct ?
    // Но проблема в том что PostConstruct не может быть одновременно @Transactional
    open fun initIfRequired() {
        if (initRequred)
            synchronized(this) {
                if (initRequred) {
                    val b = em.criteriaBuilder
                    val query = b.createQuery(AdminUser::class.java)
                    query.select(query.from(AdminUser::class.java))
                    try {
                        em.createQuery(query).maxResults
                    } catch (e: NoResultException) {
                        em.persist(AdminUser("admin", "adminadmin", null))
                    } catch (e: javax.persistence.NonUniqueResultException){
                    }

                    em.flush()
                    initRequred = false
                }
            }

    }


    @Transactional
    override fun getUserBy(username: String, password: String): User? {
        initIfRequired()
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
                    .createQuery("SELECT users.id as id, * FROM users INNER JOIN adminuser on users.id = adminuser.id WHERE login = :login")
                    .addParameter("login", username)
                    .throwOnMappingFailure(false)
                    .executeAndFetchFirst(AdminUser::class.java)

        }

//        val b = em.criteriaBuilder
//        val query = b.createQuery(AdminUser::class.java)
//        val from = query.from(AdminUser::class.java)
//        query.where(b.equal(from.get<Any>("username"), username))
//
//        try {
//            val query1 = em.createQuery(query)
//            query1.flushMode = FlushModeType.COMMIT
//            query1.maxResults = 1
//            return query1.singleResult
//        } catch (e: javax.persistence.NoResultException) {
//            return null
//        }

    }


    @Transactional
    override fun saveUser(user: User): Boolean {
        log.debug("saving user {}", user)
        if (user is Student)
            storage.saveStudent(user, null)
        else {
            em.merge(user)
        }
        return true
    }

    override fun getUsers(): List<User> {
        throw UnsupportedOperationException("getUsers")
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
