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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import ru.stankin.mj.model.user.User;
import ru.stankin.mj.model.user.UserDAO;
import ru.stankin.mj.model.user.UserInfo;

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

    private static Logger log = LogManager.getLogger(MyVaadinUI.class);

    @Inject
    CDIViewProvider viewProvider;

    @Inject
    private UserInfo user;

    @Inject
    private UserDAO userDAO;

    @Override
    protected void init(VaadinRequest request) {

        final WrappedSession session = request.getWrappedSession();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            outputStream.writeObject(session.getAttribute("com.vaadin.server.VaadinSession.VaadinServlet"));
            outputStream.close();
            log.debug("Session size:"+out.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String collect = session.getAttributeNames().stream()
                .map(s -> (s + " -> " + session.getAttribute(s))).collect(Collectors.joining("\n"));

        log.debug("Session:"+collect);

        Navigator navigator = new Navigator(this, getCurrent());
        navigator.addProvider(viewProvider);
        log.debug("user.getRoles()"+String.join(", ", user.getRoles()));

        User loginUser = null;

        final Object principal = SecurityUtils.getSubject().getPrincipal();
        log.debug("current principal {}", principal);
        if (principal != null) {
            loginUser = userDAO.getUserBy(principal.toString());
            user.setUser(loginUser);
        }

        if(!user.getRoles().contains("user")) {
            navigator.navigateTo("login");
            return;
        }
        navigator.navigateTo("");
    }

}
