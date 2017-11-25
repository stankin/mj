package ru.stankin.mj.view.utils

import com.vaadin.server.VaadinService
import com.vaadin.ui.*
import java.net.URLDecoder
import java.net.URLEncoder
import javax.servlet.http.Cookie

/**
 * Created by nickl on 03.05.15.
 */

fun showCentralWindow(ui: UI, window: Window) {
    window.setHeight(ui.height / 2, ui.heightUnits)
    window.setWidth(ui.width / 2, ui.widthUnits)
    window.center()
    ui.addWindow(window)
    }

fun AbstractOrderedLayout.addComponentExpand(c: Component, ratio: Float) {
    this.addComponent(c)
    setExpandRatio(c, ratio)
}

fun AbstractOrderedLayout.addComponentAlignment(c: Component, alignment: Alignment) {
    this.addComponent(c)
    setComponentAlignment(c, alignment)
}

object Cookies {

    val defaultMaxAge = 60 * 60 * 24 * 365

    operator fun set(name: String, value: String) {
        val cookie = VaadinService.getCurrentRequest().cookies.find { it.name == name }
                ?.apply { this.value = value }
                ?: Cookie(name, value)
        cookie.path = VaadinService.getCurrentRequest().contextPath;
        cookie.maxAge = defaultMaxAge
        VaadinService.getCurrentResponse().addCookie(cookie)
    }

    operator fun get(name: String) = VaadinService.getCurrentRequest().cookies.find { it.name == name }?.value

}

val String.urlencoded: String
    get() = URLEncoder.encode(this, "UTF-8")

val String.urldecoded: String
    get() = URLDecoder.decode(this, "UTF-8")