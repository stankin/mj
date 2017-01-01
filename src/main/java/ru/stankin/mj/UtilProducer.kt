package ru.stankin.mj

import com.sun.org.apache.xpath.internal.SourceTree
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.flywaydb.core.Flyway
import org.sql2o.Sql2o

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
    fun initDatabase() {
        log.info("Running Flyway to init database ")


        val flyway = Flyway()
        flyway.setLocations("classpath:/sql")
        flyway.dataSource = dataSource

        val properties = Properties()
        val flywayProps = javaClass.classLoader.getResourceAsStream("flyway.properties")
        if (flywayProps != null) {
            properties.load(flywayProps)
            val cleanup = properties.getProperty("flyway.cleandb")
            log.debug("cleanDbattr = {}", cleanup)
            if ("true".equals(cleanup, ignoreCase = true)) {
                log.info("cleaningdb")
                flyway.clean()
            }

        }

        flyway.migrate()
        log.info("Flyway database migration is done")

    }


    @Resource(lookup = "java:jboss/datasources/mj2")
    private lateinit var dataSource: DataSource


    @Produces
    fun defaultExecutorService(): ExecutorService = executorService

    @Produces
    @Default
    fun defaultDataSource(): DataSource = dataSource

    @Produces
    fun defaultSql2o(): Sql2o = Sql2o(dataSource)

    @Produces
    fun defaultApplicationProperties(): Properties  {
        val fileName = System.getProperty("jboss.server.config.dir")!! + "/mj.properties"

        return Properties().apply {
            FileInputStream(fileName).use { fis -> this.load(fis) }
        }
    }


}
