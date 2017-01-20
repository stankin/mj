package ru.stankin.mj.testutils

import com.vaadin.cdi.CDIViewProvider
import io.kotlintest.specs.FunSpec
import org.jboss.weld.bootstrap.api.CDI11Bootstrap
import org.jboss.weld.bootstrap.spi.Deployment
import org.jboss.weld.environment.se.Weld
import org.jboss.weld.environment.se.WeldContainer
import org.jboss.weld.injection.spi.ResourceInjectionServices
import org.jboss.weld.resources.spi.ResourceLoader
import ru.stankin.mj.UtilProducer
import java.util.*

abstract class InWeldTest : FunSpec() {

    lateinit var container: WeldContainer;

    inline fun <reified T : Any> bean() = container.select<T>(T::class.java).get()

    override fun beforeAll() {
        try {

            System.setProperty("jboss.server.config.dir", "src/test/resources/testconfig")

            val weld = object : Weld() {
                override fun createDeployment(resourceLoader: ResourceLoader?, bootstrap: CDI11Bootstrap?): Deployment {
                    val deployment = super.createDeployment(resourceLoader, bootstrap)
                    deployment.services.add(ResourceInjectionServices::class.java, JNDIResourceInjectionServices())
                    return deployment
                }
            }.apply {
                disableDiscovery()
                addPackages(true, UtilProducer::class.java)
                addPackages(true, CDIViewProvider::class.java)
            }

            container = weld.initialize()
            bean<UtilProducer>().initDatabase(Properties().apply { put("flyway.cleandb", "true") })
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun afterAll() {
        container.shutdown()
    }

}