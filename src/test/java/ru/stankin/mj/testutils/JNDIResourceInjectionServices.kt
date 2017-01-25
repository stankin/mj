package ru.stankin.mj.testutils

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import com.icegreen.greenmail.util.ServerSetupTest
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices
import org.sql2o.GenericDatasource
import ru.stankin.mj.utils.requireSysProp
import javax.annotation.Resource
import javax.enterprise.inject.Default
import javax.enterprise.inject.Produces
import javax.enterprise.inject.spi.InjectionPoint
import javax.naming.Context

@Default
class JNDIResourceInjectionServices : AbstractResourceServices() {

    val mail = GreenMail(ServerSetupTest.SMTP_IMAP)

    val context = ContextStub().apply {
        src.put("java:jboss/datasources/mj2",
                GenericDatasource(
                        requireSysProp("mj.test.pg.url"),
                        requireSysProp("mj.test.pg.user"),
                        requireSysProp("mj.test.pg.password")
                )
        )
        src.put("java:jboss/mail/Default", mail.let { it.start(); it.smtp.createSession() })
    }


    override fun getContext(): Context = context

    override fun getResourceName(injectionPoint: InjectionPoint?): String {
        val lookup = (injectionPoint?.annotated?.annotations?.find { it is Resource }as? Resource)?.lookup
        return lookup?: super.getResourceName(injectionPoint)
    }

}