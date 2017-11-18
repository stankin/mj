package ru.stankin.mj.javamigrations

import org.apache.logging.log4j.LogManager
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.utils.JSON
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*

/**
 * Created by nickl on 13.01.17.
 */
class V3_2__encryptpasswords : JdbcMigration {

    val log = LogManager.getLogger(V3_2__encryptpasswords::class.java)

    data class IdPassword(val id: Int, val password: String, val login:String)

    override fun migrate(connection: Connection) {

        val adminPasswords = connection.prepareStatement("SELECT a.id, a.password, u.login FROM adminuser a JOIN users u ON a.id = u.id")
                .use { aggregateIdPasswords(it) }

        val studentPasswords = connection.prepareStatement("SELECT s.id, s.password, u.login FROM student s JOIN users u ON s.id = u.id")
                .use { aggregateIdPasswords(it) }


        val ps = AuthenticationsStore.passwordService

        storePasswordsInAuthentication(connection,
                (adminPasswords + studentPasswords).map {
                    log.debug("enrypting ${it.password}")
                    val pswtoEnrypt = if (it.password.isEmpty()) {
                        log.debug("replacing empty password with login")
                        it.login
                    } else it.password
                    it.copy(password = ps.encryptPassword(pswtoEnrypt))
                }
        )

        //throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun storePasswordsInAuthentication(connection: Connection, passwords: List<IdPassword>) {
        connection.prepareStatement("INSERT INTO authentication (user_id, method, value) VALUES (?, ?,cast(? AS JSON))")
                .use { smt ->
                    for (idp in passwords) {
                        smt.setInt(1, idp.id)
                        smt.setString(2, "password")
                        smt.setString(3, JSON.asJson(mapOf("password" to idp.password)))
                        smt.executeUpdate();
                    }
                }
    }

    private fun aggregateIdPasswords(stmt: PreparedStatement): ArrayList<IdPassword> {
        val idPasswords = ArrayList<IdPassword>()
        val rs = stmt.executeQuery()
        while (rs.next()) {
            idPasswords.add(IdPassword(rs.getInt(1), rs.getString(2), rs.getString(3)))
        }
        return idPasswords
    }
}