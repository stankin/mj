package ru.stankin.mj.view;

import com.vaadin.server.ClientConnector;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by nickl on 01.05.15.
 */


public class AlarmHolder {

    private String title;

    ClientConnector uI;

    public AlarmHolder(String title, ClientConnector uI) {
        this.title = title;
        this.uI = uI;
    }

    private ReportWindow reportWindow;

    public void post(String text, Notification.Type type) {

        if (reportWindow == null) {
            reportWindow = new ReportWindow(title, "");
            reportWindow.addCloseListener(new Window.CloseListener() {
                @Override
                public void windowClose(Window.CloseEvent e) {
                    reportWindow = null;
                }
            });
            Utils.showCentralWindow(uI.getUI(), reportWindow);
        }

        reportWindow.body.setValue(reportWindow.body.getValue() + "\n" + text);


    }

    public void post(String join) {
        post(join, Notification.Type.TRAY_NOTIFICATION);
    }

    public void error(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter s = new PrintWriter(writer);
        e.printStackTrace(s);
        s.close();
        post(writer.toString(), Notification.Type.ERROR_MESSAGE);
    }
}

class ReportWindow extends Window {


    Label body;

    public ReportWindow(String caption, String text) {
        super(caption);
        VerticalLayout vertical = new VerticalLayout();
        vertical.setMargin(true);
        vertical.addComponent(new Label(text));
        body = new Label(text, ContentMode.PREFORMATTED);
        vertical.addComponent(body);
        vertical.setExpandRatio(body, 2);
        vertical.setSizeFull();


        this.setContent(vertical);

    }
}

