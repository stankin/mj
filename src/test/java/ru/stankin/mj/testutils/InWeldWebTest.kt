package ru.stankin.mj.testutils

import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.*
import io.undertow.servlet.util.ImmediateInstanceHandle
import org.jboss.resteasy.cdi.ResteasyCdiExtension
import org.jboss.weld.environment.se.Weld
import ru.stankin.mj.StudentsApiTest
import java.net.URL
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.Filter
import javax.servlet.Servlet

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

    open fun servlets(): List<ServletInfo> = emptyList()

    private lateinit var servlets: List<ServletInfo>

    class MappedFilterInfo(val info: FilterInfo, val mapping: String)

    inline fun <reified T : Filter> filter(path: String): MappedFilterInfo = MappedFilterInfo(Servlets.filter(T::class.java), path)

    open fun filters(): List<MappedFilterInfo> = emptyList()

    private lateinit var filters: List<MappedFilterInfo>

    inline fun <reified T : EventListener> listener(): ListenerInfo = Servlets.listener(T::class.java, WeldInstanceFactory(T::class.java))

    open fun listeners(): List<ListenerInfo> = emptyList()

    open val restApiPrefix = "/rest"

    open protected fun restClasses(): List<Class<*>> = emptyList()

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
            filters = filters()
            super.beforeAll()
            val servletBuilder = Servlets.deployment()
                    .setClassLoader(StudentsApiTest::class.java.getClassLoader())
                    .setContextPath("")
                    .setDeploymentName("mjtest.war")
                    .addServlets(servlets)
                    .apply { for(filter in this@InWeldWebTest.filters ) {
                        addFilter(filter.info)
                        addFilterUrlMapping(filter.info.name,filter.mapping, DispatcherType.REQUEST)
                    } }
                    .deployRest(restClasses(), restApiPrefix)
                    .addListener(Servlets.listener(org.jboss.weld.environment.servlet.Listener::class.java,
                            { ImmediateInstanceHandle(org.jboss.weld.environment.servlet.Listener.using(container)) }))
                    .addListeners(listeners())

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


