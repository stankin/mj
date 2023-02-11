package ru.stankin.mj.utils

import org.apache.logging.log4j.LogManager
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import java.util.*
import javax.sql.DataSource

object FlywayMigrations {

    private var log = LogManager.getLogger(FlywayMigrations::class.java)


    fun process(dataSource: DataSource, properties: Properties?) {

        val props = properties ?: flywayProperties()

        log.info("Running Flyway to init database ")

        val flyway = Flyway(FluentConfiguration()
            .locations("classpath:/sql", "classpath:/ru/stankin/mj/javamigrations")
            .dataSource(dataSource)
            .baselineOnMigrate(true))
        
        val cleanup = props.getProperty("flyway.cleandb")
        log.debug("cleanDbattr = {}", cleanup)
        if ("true".equals(cleanup, ignoreCase = true)) {
            log.info("cleaningdb")
            flyway.clean()
        }
        flyway.migrate()
        log.info("Flyway database migration is done")
    }


    private fun flywayProperties(): Properties {
        val properties = Properties()
        val flywayProps = javaClass.classLoader.getResourceAsStream("flyway.properties")
        if (flywayProps != null) {
            properties.load(flywayProps)
        }
        return properties
    }

}
