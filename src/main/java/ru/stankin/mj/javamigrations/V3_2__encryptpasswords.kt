package ru.stankin.mj.javamigrations

import kotlinx.support.jdk7.use
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import ru.stankin.mj.utils.JSON
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*

/**
 * Created by nickl on 13.01.17.
 */
class V3_2__encryptpasswords : JdbcMigration {

    data class IdPassword(val id: Int, val password: String)

    override fun migrate(connection: Connection) {

        val adminPasswords = connection.prepareStatement("SELECT id, password FROM adminuser")
                .use { aggregateIdPasswords(it) }

        val studentPasswords = connection.prepareStatement("SELECT id, password FROM student")
                .use { aggregateIdPasswords(it) }

        storePasswordsInAithentication(connection, adminPasswords + studentPasswords)

        //throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun storePasswordsInAithentication(connection: Connection, passwords: List<IdPassword>) {
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
            idPasswords.add(IdPassword(rs.getInt(1), rs.getString(2)))
        }
        return idPasswords
    }
}