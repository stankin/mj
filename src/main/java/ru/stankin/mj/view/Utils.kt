package ru.stankin.mj.view.utils

import com.vaadin.ui.*

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

