package ru.stankin.mj

import com.sun.org.apache.xpath.internal.SourceTree
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.flywaydb.core.Flyway
import org.sql2o.Sql2o
import org.sql2o.converters.Converter
import org.sql2o.quirks.NoQuirks
import ru.stankin.mj.utils.FlywayMigrations
import ru.stankin.mj.utils.requireSysProp

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Default
import javax.enterprise.inject.Produces
import javax.sql.DataSource
import java.io.IOException
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import java.io.FileInputStream
import java.sql.Array
import javax.annotation.PreDestroy
import javax.inject.Singleton


/**
 * Created by nickl on 07.01.15.
 */
@Singleton
open class UtilProducer {

    private var log = LogManager.getLogger(UtilProducer::class.java)


    private var executorService = Executors.newCachedThreadPool()


    @PostConstruct
    fun initDatabase() = initDatabase(null)

    fun initDatabase(properties: Properties?) = FlywayMigrations.process(dataSource, properties)


    @Resource(lookup = "java:jboss/datasources/mj2")
    private lateinit var dataSource: DataSource


    @Produces
    fun defaultExecutorService(): ExecutorService = executorService

    @Produces
    @Default
    fun defaultDataSource(): DataSource = dataSource

    @Produces
    fun defaultSql2o(): Sql2o = Sql2o(dataSource,
            NoQuirks(mutableMapOf<Class<*>, Converter<*>>(java.util.List::class.java to ArrayConverter())))

    @Produces
    fun defaultApplicationProperties(): Properties  {
        val fileName = requireSysProp("jboss.server.config.dir") + "/mj.properties"

        return Properties().apply {
            FileInputStream(fileName).use { fis -> this.load(fis) }
        }
    }

    @PreDestroy
    fun close(){
        executorService.shutdown()
    }



}


class ArrayConverter:Converter<List<String>> {

    override fun convert(a: Any?): List<String> {

        return when(a){
            is Array -> {
                (a.array as kotlin.Array<String?>).filterNotNull()
            }

            else -> throw UnsupportedOperationException("converting $a of class ${a?.javaClass} to List<String>")
        }

    }

    override fun toDatabaseParam(`val`: List<String>?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}