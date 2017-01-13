package ru.stankin.mj.testutils

import org.jboss.weld.injection.spi.helpers.AbstractResourceServices
import org.sql2o.GenericDatasource
import javax.annotation.Resource
import javax.enterprise.inject.Default
import javax.enterprise.inject.spi.InjectionPoint
import javax.naming.Context

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
        val lookup = (injectionPoint?.annotated?.annotations?.find { it is Resource }as? Resource)?.lookup
        return lookup?: super.getResourceName(injectionPoint)
    }
}