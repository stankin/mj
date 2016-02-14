package ru.stankin.mj.view;

import com.vaadin.annotations.Push;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIView;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import ru.stankin.mj.User;
import ru.stankin.mj.UserDAO;
import ru.stankin.mj.UserInfo;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;


@CDIView("login")
public class LoginView extends CustomComponent implements View, ClickListener {

    @Inject
    private UserInfo user;

    @Inject
    private UserDAO userDAO;

    private TextField usernameField;
    private Label errorLabel;
    private PasswordField passwordField;
    private Button loginButton;

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
        layout.addComponent(loginButton);


        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request instanceof VaadinServletRequest) {
            HttpServletRequest httpRequest = ((VaadinServletRequest)request).getHttpServletRequest();
            String userAgent = httpRequest.getHeader("User-Agent").toLowerCase();
            if (userAgent.contains("android")) {
                layout.addComponent(new Label("<a href=\"https://play.google.com/store/apps/details?id=maxim.ru.modulejournal\">Приложение для Android</a>",  ContentMode.HTML));
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

        User loginUser = userDAO.getUserBy(username, password);
        if (loginUser == null) {
            errorLabel.setValue("Неверный пароль.\nОбратитесь в деканат, если не знаете пароль.");
            errorLabel.setVisible(true);
//            passwordField.setComponentError(new UserError("Неверный пароль"));
//            new Notification("Неверный пароль", Notification.TYPE_ERROR_MESSAGE)
//                    .show(getUI().getPage());
            return;
        }

        user.setUser(loginUser);
        this.getUI().getNavigator().navigateTo("");

    }
}