package ru.stankin.mj

import com.sun.org.apache.xpath.internal.SourceTree
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.flywaydb.core.Flyway
import org.sql2o.Sql2o
import ru.stankin.mj.utils.FlywayMigrations

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



/**
 * Created by nickl on 07.01.15.
 */
@ApplicationScoped
open class UtilProducer {

    private var log = LogManager.getLogger(UtilProducer::class.java)


    private var executorService = Executors.newCachedThreadPool()


    @PostConstruct
    open fun initDatabase() = initDatabase(null)

    open fun initDatabase(properties: Properties?) = FlywayMigrations.process(dataSource, properties)


    @Resource(lookup = "java:jboss/datasources/mj2")
    private lateinit var dataSource: DataSource


    @Produces
    open fun defaultExecutorService(): ExecutorService = executorService

    @Produces
    @Default
    open fun defaultDataSource(): DataSource = dataSource

    @Produces
    open fun defaultSql2o(): Sql2o = Sql2o(dataSource)

    @Produces
    open fun defaultApplicationProperties(): Properties  {
        val fileName = System.getProperty("jboss.server.config.dir")!! + "/mj.properties"

        return Properties().apply {
            FileInputStream(fileName).use { fis -> this.load(fis) }
        }
    }


}


