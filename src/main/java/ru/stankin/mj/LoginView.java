package ru.stankin.mj;

import com.vaadin.annotations.Push;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIView;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

import javax.inject.Inject;


@CDIView("login")
public class LoginView extends CustomComponent implements View, ClickListener {

    @Inject
    private UserInfo user;

    @Inject
    private UserDAO userDAO;

    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;

    @Override
    public void enter(ViewChangeEvent event) {

        usernameField = new TextField("Логин");
        passwordField = new PasswordField("Пароль");
        loginButton = new Button("Вход");
        loginButton.addClickListener(this);
        loginButton.setClickShortcut(KeyCode.ENTER);


        VerticalLayout layout = new VerticalLayout();

        //layout.setWidth("100%");


        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        Label label = new Label("<b>Вход</b>",  ContentMode.HTML);
        layout.addComponent(label);
        layout.addComponent(usernameField);
        layout.addComponent(passwordField);
        layout.addComponent(loginButton);

        Panel panel = new Panel();
        panel.setWidthUndefined();
        panel.setContent(layout);
        VerticalLayout layout1 = new VerticalLayout();
        layout1.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        layout1.addComponent(panel);
        setCompositionRoot(layout1);
    }

    @Override
    public void buttonClick(ClickEvent event) {
        String username = usernameField.getValue();
        String password = passwordField.getValue();

        User loginUser = userDAO.getUserBy(username, password);
        if (loginUser == null) {
            new Notification("Неверный пароль", Notification.TYPE_ERROR_MESSAGE)
                    .show(getUI().getPage());
            return;
        }

        user.setUser(loginUser);
        this.getUI().getNavigator().navigateTo("");

    }
}