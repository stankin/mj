package ru.stankin.mj.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import ru.stankin.mj.model.user.User;
import ru.stankin.mj.model.user.UserDAO;
import ru.stankin.mj.model.user.UserInfo;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.UUID;


@CDIView("login")
public class LoginView extends CustomComponent implements View, ClickListener {

    @Inject
    private UserInfo user;

    @Inject
    private UserDAO userDAO;

    @Inject
    private SecurityManager securityManager;

    private TextField usernameField;
    private Label errorLabel;
    private PasswordField passwordField;
    private Button loginButton;
    private CheckBox rememberMeCbx;

    @Override
    public void enter(ViewChangeEvent event) {

        usernameField = new TextField("Логин");
        usernameField.addFocusListener(event1 -> errorLabel.setVisible(false));
        errorLabel = new Label();
        errorLabel.addStyleName("err-label");
        errorLabel.setVisible(false);
        errorLabel.setWidth(146,Unit.PIXELS);
        passwordField = new PasswordField("Пароль");
        passwordField.addFocusListener(event1 -> {errorLabel.setVisible(false); passwordField.focus();});
        loginButton = new Button("Вход");
        loginButton.addClickListener(this);
        loginButton.setClickShortcut(KeyCode.ENTER);
        setHeight("100%");
        rememberMeCbx = new CheckBox("Запомнить");
        rememberMeCbx.setBuffered(true);

        VerticalLayout layout = new VerticalLayout();

        //layout.setWidth("100%");


        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        Label label = new Label("<b>Вход</b>",  ContentMode.HTML);
        layout.addComponent(label);
        layout.addComponent(usernameField);
        layout.addComponent(errorLabel);
        layout.addComponent(passwordField);
        layout.addComponent(rememberMeCbx);
        layout.addComponent(new Label("<a href='forceLogin?client_name=Google2Client'>Войти через Google</a>", ContentMode.HTML));
        layout.addComponent(new Label("<a href='forceLogin?client_name=VkClient'>Войти через Vkontakte</a>", ContentMode.HTML));
        layout.addComponent(loginButton);


        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request instanceof VaadinServletRequest) {
            HttpServletRequest httpRequest = ((VaadinServletRequest)request).getHttpServletRequest();
            String userAgent = httpRequest.getHeader("User-Agent").toLowerCase();
            if (userAgent.contains("android")) {
                layout.addComponent(new Label("<a href=\"https://play.google.com/store/apps/details?id=ru.modulejournal\">Приложение для Android</a>",  ContentMode.HTML));
            }

        }

        Panel panel = new Panel();
        panel.setWidthUndefined();
        panel.setContent(layout);
        VerticalLayout loginLayout = new VerticalLayout();
        loginLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        loginLayout.addComponent(panel);


        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.addComponent(loginLayout);
        verticalLayout.setExpandRatio(loginLayout, 1);

        Label spacer = new Label();
        spacer.setSizeFull();
        verticalLayout.addComponent(spacer);
        verticalLayout.setExpandRatio(spacer, 1);

        setCompositionRoot(verticalLayout);

        //setCompositionRoot(loginLayout);
    }

    @Override
    public void buttonClick(ClickEvent event) {
        String username = usernameField.getValue();
        String password = passwordField.getValue();


        try {
            SecurityUtils.getSubject().login(new UsernamePasswordToken(username, password, rememberMeCbx.getValue()));

            User loginUser = Objects.requireNonNull(userDAO.getUserBy(username, password), "user cant be null") ;

            user.setUser(loginUser);
            this.getUI().getNavigator().navigateTo("");

        } catch (AuthenticationException e){
                errorLabel.setValue("Неверный пароль.\nОбратитесь в деканат, если не знаете пароль.");
                errorLabel.setVisible(true);
        }

    }
}