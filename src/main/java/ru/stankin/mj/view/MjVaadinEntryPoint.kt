package ru.stankin.mj.view

import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.cdi.CDIUI
import com.vaadin.cdi.CDIView
import com.vaadin.cdi.CDIViewProvider
import com.vaadin.navigator.Navigator
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinService
import com.vaadin.server.WrappedSession
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import io.buji.pac4j.subject.Pac4jPrincipal
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.shiro.SecurityUtils
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.user.User
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

        if (SecurityUtils.getSubject().hasRole(MjRoles.UNBINDED_OAUTH)) {
            navigator.navigateTo("bindoauth")
            return
        }

        if (!SecurityUtils.getSubject().hasRole(MjRoles.USER)) {
            navigator.navigateTo("login")
            return
        }
        navigator.navigateTo("")
    }

}


@CDIView("bindoauth")
class BindOauthView():LoginView() {

    @Inject
    lateinit var auth:AuthenticationsStore

    override fun titleLabel(): Label = Label("Внешнаяя аутентификация не привязана ни к какому аккаунту.<br/>" +
            "Введите логин и пароль чтобы осуществить привязку к аккаунту", ContentMode.HTML)


    override fun doLogin(username: String?, password: String?) {

        val pac4jPrincipal = SecurityUtils.getSubject().principals.oneByType(Pac4jPrincipal::class.java)
        super.doLogin(username, password)
        auth.assignProfileToUser(MjRoles.getUser()!!.id, pac4jPrincipal.profile)
    }

    override fun androidSuggest(layout: VerticalLayout?) {}

    override fun additionalButtons(layout: VerticalLayout) {}

    override fun getLoginButton(): Component {
        return HorizontalLayout(
                super.getLoginButton(),
                Button("Отмена", { a ->
                    SecurityUtils.getSubject().logout()
                    this.ui.navigator.navigateTo("login")
                }))
    }
}