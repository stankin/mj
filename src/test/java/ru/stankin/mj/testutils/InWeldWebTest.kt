package ru.stankin.mj.testutils

import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentInfo
import io.undertow.servlet.api.InstanceFactory
import io.undertow.servlet.api.InstanceHandle
import io.undertow.servlet.api.ServletInfo
import io.undertow.servlet.util.ImmediateInstanceFactory
import io.undertow.servlet.util.ImmediateInstanceHandle
import org.jboss.resteasy.cdi.CdiInjectorFactory
import org.jboss.resteasy.cdi.ResteasyCdiExtension
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher
import org.jboss.resteasy.spi.ResteasyDeployment
import org.jboss.weld.environment.se.Weld
import ru.stankin.mj.MyServlet
import ru.stankin.mj.WebTest
import ru.stankin.mj.rested.StudentApi
import java.net.URL
import javax.servlet.Servlet
import kotlin.reflect.KClass


/**
 * Created by nickl on 11.03.17.
 */
abstract class InWeldWebTest : InWeldTest() {


    lateinit var server: Undertow

    val port = 8080

    inline fun <reified T : Servlet> servlet(): ServletInfo = Servlets.servlet(
            T::class.java.name,
            T::class.java,
            WeldInstanceFactory(T::class.java)
    )

    inner class WeldInstanceFactory<T>(val clazz: Class<T>) : InstanceFactory<T> {
        override fun createInstance(): InstanceHandle<T> =
                ImmediateInstanceHandle<T>(container.select<T>(clazz).get())
    }

    inline fun <reified T : Servlet> servlet(path: String): ServletInfo = servlet<T>().addMapping(path)

    abstract fun servlets(): List<ServletInfo>

    open val restApiPrefix = "/rest"

    open protected fun restClasses(): List<Class<*>> = emptyList()

    private lateinit var servlets: List<ServletInfo>

    override fun addWeldElems(weld: Weld) {
        super.addWeldElems(weld)
        weld.addExtension(ResteasyCdiExtension())
        for (info in servlets) {
            weld.addBeanClass(info.servletClass)
        }
    }

    fun serverURL(spec: String): URL = URL("http", "localhost", port, spec)

    fun restURL(spec: String): URL = URL(serverURL(""), restApiPrefix + spec)


    override fun beforeAll() {
        try {
            servlets = servlets()
            super.beforeAll()
            val servletBuilder = Servlets.deployment()
                    .setClassLoader(WebTest::class.java.getClassLoader())
                    .setContextPath("")
                    .setDeploymentName("mjtest.war")
                    .addServlets(servlets)
                    .deployRest(restClasses(), restApiPrefix)
                    .addListener(Servlets.listener(org.jboss.weld.environment.servlet.Listener::class.java,
                            { ImmediateInstanceHandle(org.jboss.weld.environment.servlet.Listener.using(container)) }))

            val manager = Servlets.defaultContainer().addDeployment(servletBuilder)
            manager.deploy()

            val servletHandler = manager.start()
            server = Undertow.builder()
                    .addHttpListener(port, "localhost")
                    .setHandler(servletHandler)
                    .build()
            server.start()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun afterAll() {
        server.stop()
        super.afterAll()
    }
}


