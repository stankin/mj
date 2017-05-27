package ru.stankin.mj.view;

import com.vaadin.cdi.CDIView;
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
import com.vaadin.ui.themes.BaseTheme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.jetbrains.annotations.NotNull;
import ru.stankin.mj.UserActionException;
import ru.stankin.mj.model.UserResolver;
import ru.stankin.mj.model.user.PasswordRecoveryService;
import ru.stankin.mj.model.user.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;


@CDIView("login")
public class LoginView extends CustomComponent implements View {

    private static final Logger logger = LogManager.getLogger(LoginView.class);

    @Inject
    protected PasswordRecoveryService recoveryService;

    @Inject
    protected UserResolver userResolver;


    protected TextField usernameField;
    protected Label errorLabel;
    protected PasswordField passwordField;
    protected Button loginButton;
    protected Button recoveryButton;
    protected CheckBox rememberMeCbx;

    @Override
    public void enter(ViewChangeEvent event) {

        usernameField = new TextField("Логин");
        usernameField.addFocusListener(event1 -> {
            errorLabel.setVisible(false);
            usernameField.setComponentError(null);
        });
        errorLabel = new Label();
        errorLabel.addStyleName("err-label");
        errorLabel.setVisible(false);
        errorLabel.setWidth(146,Unit.PIXELS);
        passwordField = new PasswordField("Пароль");
        passwordField.addFocusListener(event1 -> {errorLabel.setVisible(false); passwordField.focus();});
        recoveryButton = new Button("Восстановить пароль");
        recoveryButton.addStyleName(BaseTheme.BUTTON_LINK);
        recoveryButton.addClickListener(this::recoveryButtonClick);
        loginButton = new Button("Вход");
        loginButton.addClickListener(this::loginButtonClick);
        loginButton.setClickShortcut(KeyCode.ENTER);
        setHeight("100%");
        rememberMeCbx = new CheckBox("Запомнить");
        rememberMeCbx.setBuffered(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        layout.addComponent(titleLabel());
        layout.addComponent(usernameField);
        layout.addComponent(errorLabel);
        layout.addComponent(passwordField);
        additionalButtons(layout);
        layout.addComponent(getLoginButton());

        Panel panel = new Panel();
        panel.setWidthUndefined();
        panel.setContent(layout);
        VerticalLayout loginLayout = new VerticalLayout();
        loginLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        loginLayout.addComponent(panel);
        VerticalLayout links = new VerticalLayout(
                new Label("<a href='https://play.google.com/store/apps/details?id=ru.modulejournal&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img style='height:40px;' alt='Доступно в Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/ru_badge_web_generic.png'/></a>", ContentMode.HTML)
                ,
                new Label("<div style='padding: 0px 10px 10px 5px'><a href=\"https://itunes.apple.com/us/app/%D1%81%D1%82%D0%B0%D0%BD%D0%BA%D0%B8%D0%BD/id1220812657?mt=8\" style=\"display:inline-block;overflow:hidden;background:url(//linkmaker.itunes.apple.com/assets/shared/badges/ru-ru/appstore-lrg.svg) no-repeat;width:95px;height:30px;background-size:contain;\"></a></div>", ContentMode.HTML)
        );
        links.setWidthUndefined();
        loginLayout.addComponent(links);
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.addComponent(loginLayout);
        verticalLayout.setExpandRatio(loginLayout, 1);

        Label spacer = new Label();
        spacer.setSizeFull();
        verticalLayout.addComponent(spacer);
        verticalLayout.setExpandRatio(spacer, 1);

        setCompositionRoot(verticalLayout);
    }

    private void recoveryButtonClick(ClickEvent clickEvent) {
        User user = userResolver.getUserBy(usernameField.getValue());
        if(user == null)
        {
            usernameField.setComponentError(new UserError("Пользователь не найден"));
            Notification notification = new Notification("Не выполнено",
                    "Укажите в поле \"Логин\" имя пользователя (номер студенческого билета) ",
                    Notification.Type.WARNING_MESSAGE);

            notification.setDelayMsec(10000);
            notification.show(this.getUI().getPage());
            return;
        }

        try {
            recoveryService.sendRecovery(user);
            Notification notification = new Notification("Выполнено",
                    "Cсылка для восстановления пароля была отправлена на почту." +
                            " Если что-то пошло не так - обратитесь в деканат.",
                    Notification.Type.HUMANIZED_MESSAGE);

            notification.setDelayMsec(10000);
            notification.show(this.getUI().getPage());
        } catch (UserActionException e) {
            Notification notification = new Notification("Не выполнено",
                    e.getLocalizedMessage(),
                    Notification.Type.WARNING_MESSAGE);

            notification.setDelayMsec(10000);
            notification.show(this.getUI().getPage());
        }


    }

    protected Component getLoginButton() {
        VerticalLayout components = new VerticalLayout();
        components.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        components.addComponents(loginButton, recoveryButton);
        return components;
    }

    @NotNull
    protected Label titleLabel() {
        return new Label("<b>Вход</b>",  ContentMode.HTML);
    }

    protected void androidSuggest(VerticalLayout layout) {
    }

    protected void additionalButtons(VerticalLayout layout) {
        //layout.addComponent(rememberMeCbx);
        layout.addComponent(new Label("Войти через: <br/><a href='forceLogin?client_name=Google2Client'>Google</a>" +
                " <a href='forceLogin?client_name=VkClient'>ВКонтакте</a>" +
                " <a href='forceLogin?client_name=YandexClient'>Яндекс</a>", ContentMode.HTML));
    }


    public void loginButtonClick(ClickEvent event) {
        String username = usernameField.getValue();
        String password = passwordField.getValue();


        try {
            doLogin(username, password);

            URI afterLogin = (URI) SecurityUtils.getSubject().getSession().removeAttribute("redirectAfterLogin");
            logger.debug("afterLogin={}", afterLogin);
            if (afterLogin != null)
                this.getUI().getPage().setLocation(afterLogin.toString());
            else
                this.getUI().getPage().setLocation("");

        } catch (AuthenticationException e) {
            errorLabel.setValue("Неверный пароль.\nОбратитесь в деканат, если не знаете пароль.");
            errorLabel.setVisible(true);
        }

    }

    protected void doLogin(String username, String password) {
        SecurityUtils.getSubject().login(new UsernamePasswordToken(username, password, rememberMeCbx.getValue()));
    }
}