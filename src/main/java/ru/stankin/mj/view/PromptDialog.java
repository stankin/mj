package ru.stankin.mj.view;


import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

import java.util.function.Function;

/**
 * Created by nickl-mac on 18.10.15.
 */
public class PromptDialog extends Window {

    private boolean processed = false;

    public PromptDialog(String caption, String text, java.util.function.Consumer<String> callback ) {
        super(caption);
        VerticalLayout vertical = new VerticalLayout();
        vertical.setMargin(true);
        //vertical.addComponent(new Label(text));
        TextField textField = new TextField(text);
        vertical.addComponent(textField);
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.addComponent(new Button("OK", event -> {
            callback.accept(textField.getValue());
            processed = true;
            this.close();
        }));
        horizontalLayout.addComponent(new Button("Cancel", event -> {
            callback.accept(null);
            processed = true;
            this.close();
        }));
        vertical.addComponent(horizontalLayout);
        //vertical.addComponent(new );
        //vertical.setExpandRatio(body, 2);
        //vertical.setSizeFull();
        this.addCloseListener(e -> {
            if(!processed){
                callback.accept(null);
            }
        });


        this.setContent(vertical);
        this.center();
        //this.setResizable(false);
        //System.out.println("vertical:"+vertical.getHeight() + " " + vertical.getWidth());
        //this.setHeight(50, Unit.PIXELS);
        //this.setWidth(100, Unit.PIXELS);
    }

    public static void prompt(UI ui, String caption, String text, java.util.function.Consumer<String> callback ){
        PromptDialog dialog = new PromptDialog(caption, text, callback);
        dialog.setModal(true);
        dialog.center();
        ui.addWindow(dialog);
       // Utils.showCentralWindow(ui, dialog);
    }

}
