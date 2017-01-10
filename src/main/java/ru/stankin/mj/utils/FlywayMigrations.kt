package ru.stankin.mj.utils

import org.apache.logging.log4j.LogManager
import org.flywaydb.core.Flyway
import java.util.*
import javax.sql.DataSource

object FlywayMigrations {

    private var log = LogManager.getLogger(FlywayMigrations::class.java)


    fun process(dataSource: DataSource) {
        log.info("Running Flyway to init database ")


        val flyway = Flyway()
        flyway.setLocations("classpath:/sql")
        flyway.dataSource = dataSource

        val properties = flywayProperties()

        val cleanup = properties.getProperty("flyway.cleandb")
        log.debug("cleanDbattr = {}", cleanup)
        if ("true".equals(cleanup, ignoreCase = true)) {
            log.info("cleaningdb")
            flyway.clean()
        }
        flyway.isBaselineOnMigrate = true //TODO: Remove it after full switch to Flyway
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
