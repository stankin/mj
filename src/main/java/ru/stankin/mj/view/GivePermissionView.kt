package ru.stankin.mj.view

import com.vaadin.cdi.CDIView
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.OAuthProviderService
import ru.stankin.mj.rested.security.MjRoles
import ru.stankin.mj.utils.restutils.queryParams
import ru.stankin.mj.utils.restutils.uriBuilder
import java.net.URI
import javax.inject.Inject

/**
 * Created by nickl on 16.03.17.
 */
@CDIView("givepermission")
class GivePermissionView : CustomComponent(), View {

    private val log = LogManager.getLogger(OAuthProviderService::class.java)

    @Inject
    private lateinit var prov: OAuthProvider

    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {

        centredPanel(VerticalLayout().apply {
            setSizeFull()
            setMargin(true)
            isSpacing = true
            defaultComponentAlignment = Alignment.MIDDLE_CENTER
            val uri = SecurityUtils.getSubject().session.removeAttribute("redirectAfterLogin")!! as URI
            val clientId = uri.queryParams["client_id"]!!
            val consumer = prov.getConsumer(clientId)!!

            addComponent(Label("<b>Запрос прав доступа для ${consumer.serviceName}</b>", ContentMode.HTML))
            addComponent(Label("""Сервис ${consumer.serviceName} получит доступ к вашим персональным данным,
            таким как:
            <ul>
              <li>Номер студенческого билета</li>
              <li>Имя и фамилия</li>
              <li>Номер группы</li>
            </ul>""", ContentMode.HTML))

            log.debug("redirect uri {}", uri)

            addComponent(HorizontalLayout(
                    Button("OK", { e ->
                        prov.addUserPermission(clientId, MjRoles.getUser()!!.id.toLong())
                        this.ui.page.location = uri
                    }),
                    Button("Отмена", { e ->
                        val redirectTarget = uri
                        prov.removeUserPermission(clientId, MjRoles.getUser()!!.id.toLong())
                        this.ui.page.location = uriBuilder(redirectTarget) { this.queryParam("error", "cancelled") }
                    })
            ))

        })

    }

    private fun centredPanel(content: Component) {
        compositionRoot =
                VerticalLayout().apply {
                    setSizeFull();
                    addComponentExpand(VerticalLayout().apply {
                        defaultComponentAlignment = Alignment.MIDDLE_CENTER
                        addComponent(Panel().apply {
                            setWidthUndefined()
                            this.content = content
                        })
                    }, 1.0f)

                    addComponentExpand(Label().apply {
                        setSizeFull()
                    }, 1.0f)
                }
    }

    private fun AbstractOrderedLayout.addComponentExpand(c: Component, ratio: Float) {
        this.addComponent(c)
        setExpandRatio(c, ratio)
    }
}