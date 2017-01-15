package ru.stankin.mj.view

import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.cdi.CDIUI
import com.vaadin.cdi.CDIViewProvider
import com.vaadin.navigator.Navigator
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinService
import com.vaadin.server.WrappedSession
import com.vaadin.ui.UI
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.shiro.SecurityUtils
import ru.stankin.mj.model.user.User
import ru.stankin.mj.model.user.UserDAO
import ru.stankin.mj.rested.security.MjRoles

import javax.inject.Inject
import javax.servlet.http.Cookie
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.stream.Collectors

@Theme("modules-journal")
@CDIUI("")
//@SessionScoped
//@PreserveOnRefresh
//@UIScoped
@Widgetset("ru.stankin.mj.WidgetSet")
//@Push
class MjVaadinEntryPoint : UI() {

    @Inject
    private lateinit var viewProvider: CDIViewProvider

    private val log = LogManager.getLogger(MjVaadinEntryPoint::class.java)

    override fun init(request: VaadinRequest) {

        val session = request.wrappedSession

        try {
            val out = ByteArrayOutputStream()
            val outputStream = ObjectOutputStream(out)
            outputStream.writeObject(session.getAttribute("com.vaadin.server.VaadinSession.VaadinServlet"))
            outputStream.close()
            log.debug("Session size:" + out.size())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val collect = session.attributeNames
                .map({ s -> s + " -> " + session.getAttribute(s) }).joinToString("\n")

        log.debug("Session:" + collect)

        val navigator = Navigator(this, UI.getCurrent())
        navigator.addProvider(viewProvider)

        if (!SecurityUtils.getSubject().hasRole(MjRoles.USER)) {
            navigator.navigateTo("login")
            return
        }
        navigator.navigateTo("")
    }

}
