package ru.stankin.mj.view;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Created by nickl on 03.05.15.
 */
public class Utils {
    static void showCentralWindow(UI ui, Window window) {
        window.setHeight(ui.getHeight() / 2, ui.getHeightUnits());
        window.setWidth(ui.getWidth() / 2, ui.getWidthUnits());
        window.center();
        ui.addWindow(window);
    }
}
