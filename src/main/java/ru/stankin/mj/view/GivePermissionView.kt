package ru.stankin.mj.view

import com.vaadin.cdi.CDIView
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*

/**
 * Created by nickl on 16.03.17.
 */
@CDIView("givepermission")
class GivePermissionView : CustomComponent(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {

        centredPanel(VerticalLayout().apply {
            setSizeFull()
            setMargin(true)
            isSpacing = true
            defaultComponentAlignment = Alignment.MIDDLE_CENTER
            addComponent(Label("<b>Запрос прав доступа</b>", ContentMode.HTML))
            addComponent(Label("""Запрос прав доступа""", ContentMode.HTML))
            addComponent(Button("OK"))
        })

    }

    private fun centredPanel(content: Component) {
        compositionRoot =
                VerticalLayout().apply r1@ {
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