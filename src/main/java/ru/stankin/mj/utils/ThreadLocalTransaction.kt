package ru.stankin.mj.utils

import kotlinx.support.jdk7.use
import org.sql2o.Connection
import org.sql2o.Sql2o
import org.sql2o.connectionsources.ConnectionSource
import org.sql2o.connectionsources.ConnectionSources.join

/**
 * Created by nickl on 10.01.17.
 */
object ThreadLocalTransaction {


    private val connection = ThreadLocal<Connection>()


    fun get(): ConnectionSource? = connection.get()?.jdbcConnection?.let { join(it) }


    fun <T> within(sql2o: Sql2o, f: () -> T): T {
        val prev = connection.get()
        try {
            return sql2o.beginTransaction().use { t ->

                connection.set(t)
                val result = f()
                t.commit()
                result
            }

        } finally {
            connection.set(prev)
        }
    }


}