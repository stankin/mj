package ru.stankin.mj.testutils

import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.InstanceFactory
import io.undertow.servlet.api.InstanceHandle
import io.undertow.servlet.api.ServletInfo
import io.undertow.servlet.util.ImmediateInstanceFactory
import io.undertow.servlet.util.ImmediateInstanceHandle
import org.jboss.weld.environment.se.Weld
import ru.stankin.mj.MyServlet
import ru.stankin.mj.WebTest
import javax.servlet.Servlet

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

    private lateinit var servlets: List<ServletInfo>

    override fun addWeldElems(weld: Weld) {
        super.addWeldElems(weld)
        for (info in servlets) {
            weld.addBeanClass(info.servletClass)
        }
    }

    override fun beforeAll() {
        try {
            servlets = servlets()
            super.beforeAll()
            val servletBuilder = Servlets.deployment()
                    .setClassLoader(WebTest::class.java.getClassLoader())
                    .setContextPath("mjtest")
                    .setDeploymentName("mjtest.war")
                    .addServlets(servlets)

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