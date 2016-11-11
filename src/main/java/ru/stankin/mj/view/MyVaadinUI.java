package ru.stankin.mj.view;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;
import ru.stankin.mj.model.User;
import ru.stankin.mj.UserDAO;
import ru.stankin.mj.UserInfo;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

    @Inject
    private UserDAO userDAO;

    @Override
    protected void init(VaadinRequest request) {

        final WrappedSession session = request.getWrappedSession();
        System.out.println("MyVaadinUI init");

        String cook = null;

        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();
        for (Cookie cookie : cookies) {

            if (("ModuleZhurnal".equals(cookie.getName()))) {
                cookie.setMaxAge(60 * 60 * 24 * 30 * 24);
                cook = cookie.getValue();


            }
        }


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

        User loginUser = null;

        if (cook != null) {
            loginUser = userDAO.getUserCookie(cook);
        }

        if (loginUser != null) {
            user.setUser(loginUser);
            this.getUI().getNavigator().navigateTo("");
            return;
        }

        if(!user.getRoles().contains("user")) {
            navigator.navigateTo("login");
            return;
        }
        navigator.navigateTo("");



        //navigator.navigateTo("login");
    }

}
