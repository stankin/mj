package ru.stankin.mj.view;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.cdi.UIScoped;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import ru.stankin.mj.UserInfo;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.stream.Collectors;

@Theme("modules-journal")
@CDIUI("")
//@Push
@SuppressWarnings("serial")
//@SessionScoped
//@PreserveOnRefresh
//@UIScoped
@Widgetset("ru.stankin.mj.WidgetSet")
public class MyVaadinUI extends UI {

    @Inject
    CDIViewProvider viewProvider;

    @Inject
    private UserInfo user;

    @Override
    protected void init(VaadinRequest request) {

        final WrappedSession session = request.getWrappedSession();
        System.out.println("MyVaadinUI init");



        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            outputStream.writeObject(session.getAttribute("com.vaadin.server.VaadinSession.VaadinServlet"));
            outputStream.close();
            System.out.println("Session size:"+out.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String collect = session.getAttributeNames().stream()
                .map(s -> (s + " -> " + session.getAttribute(s))).collect(Collectors.joining("\n"));

        System.out.println("Session:"+collect);

        Navigator navigator = new Navigator(this, getCurrent());
        navigator.addProvider(viewProvider);
        System.out.println("user.getRoles()"+String.join(", ", user.getRoles()));
        if(!user.getRoles().contains("user")) {
            navigator.navigateTo("login");
            return;
        }
        navigator.navigateTo("");



        //navigator.navigateTo("login");
    }

}
