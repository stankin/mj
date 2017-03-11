package ru.stankin.mj.testutils

import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentInfo
import org.jboss.resteasy.cdi.CdiInjectorFactory
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher
import org.jboss.resteasy.spi.ResteasyDeployment

fun DeploymentInfo.deployRest(kClasss: List<Class<*>>, restApiPrefix: String?): DeploymentInfo {
    if (kClasss.isEmpty())
        return this


    val rd = ResteasyDeployment()
    rd.injectorFactoryClass = CdiInjectorFactory::class.java.name
    rd.scannedResourceClasses = kClasss.map { it.name }
    return this.reastEasyDeployment(rd, restApiPrefix)
}

fun DeploymentInfo.reastEasyDeployment(deployment: ResteasyDeployment, mapping: String?): DeploymentInfo {
    var mapping = mapping
    if (mapping == null) mapping = "/"
    if (!mapping.startsWith("/")) mapping = "/" + mapping
    if (!mapping.endsWith("/")) mapping += "/"
    mapping = mapping + "*"
    var prefix: String? = null
    if (mapping != "/*") prefix = mapping.substring(0, mapping.length - 2)
    val resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher::class.java)
            .setAsyncSupported(true)
            .setLoadOnStartup(1)
            .addMapping(mapping)
    if (prefix != null) resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix)

    return addServletContextAttribute(ResteasyDeployment::class.java.name, deployment)
            .addServlet(
                    resteasyServlet
            )
}
