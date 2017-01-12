package ru.stankin.mj

import com.vaadin.cdi.CDIViewProvider
import io.kotlintest.matchers.be
import io.kotlintest.specs.FunSpec
import org.jboss.weld.bootstrap.api.CDI11Bootstrap
import org.jboss.weld.bootstrap.spi.Deployment
import org.jboss.weld.environment.se.Weld
import org.jboss.weld.injection.spi.ResourceInjectionServices
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices
import org.jboss.weld.manager.api.WeldManager
import org.jboss.weld.resources.spi.ResourceLoader
import org.sql2o.GenericDatasource
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.testutils.ContextStub
import java.util.*
import javax.annotation.Resource
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Default
import javax.enterprise.inject.Produces
import javax.enterprise.inject.spi.InjectionPoint
import javax.inject.Singleton
import javax.naming.*

/**
 * Created by nickl on 11.01.17.
 */
class AuthTest : FunSpec() {
    init {
        test("String.length should return the length of the string") {
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

            //.addBeanClass(JNDIResourceInjectionServices::class.java)


            val container = weld.initialize()

            val userResolver = container.select(UserResolver::class.java).get()

            val admin = userResolver.getUserBy("admin")
            checkNotNull(admin)
            println(admin)

            container.shutdown()
        }
    }
}



@Default
class JNDIResourceInjectionServices : AbstractResourceServices() {

    val context = ContextStub().apply {
        src.put("java:jboss/datasources/mj2",
                GenericDatasource(
                        System.getProperty("mj.test.pg.url")!!,
                        System.getProperty("mj.test.pg.user")!!,
                        System.getProperty("mj.test.pg.password")!!
                )
        )
    }


    override fun getContext(): Context = context

    override fun getResourceName(injectionPoint: InjectionPoint?): String {
        sequenceOf(1)
        val lookup = (injectionPoint?.annotated?.annotations?.find { it is Resource }as? Resource)?.lookup
        return lookup?: super.getResourceName(injectionPoint)
    }
}