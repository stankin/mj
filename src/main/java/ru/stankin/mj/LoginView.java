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

    public static String YANDEX_METRIC = "<!-- Yandex.Metrika informer --><a href=\"https://metrika.yandex.ru/stat/?id=29801259&amp;from=informer\" target=\"_blank\" rel=\"nofollow\"><img src=\"//bs.yandex.ru/informer/29801259/1_0_FFFFFFFF_FFFFFFFF_0_uniques\" style=\"width:80px; height:15px; border:0;\" alt=\"Яндекс.Метрика\" title=\"Яндекс.Метрика: данные за сегодня (уникальные посетители)\" /></a><!-- /Yandex.Metrika informer --><!-- Yandex.Metrika counter --><script type=\"text/javascript\">(function (d, w, c) { (w[c] = w[c] || []).push(function() { try { w.yaCounter29801259 = new Ya.Metrika({id:29801259, clickmap:true, trackLinks:true, accurateTrackBounce:true}); } catch(e) { } }); var n = d.getElementsByTagName(\"script\")[0], s = d.createElement(\"script\"), f = function () { n.parentNode.insertBefore(s, n); }; s.type = \"text/javascript\"; s.async = true; s.src = (d.location.protocol == \"https:\" ? \"https:\" : \"http:\") + \"//mc.yandex.ru/metrika/watch.js\"; if (w.opera == \"[object Opera]\") { d.addEventListener(\"DOMContentLoaded\", f, false); } else { f(); } })(document, window, \"yandex_metrika_callbacks\");</script><noscript><div><img src=\"//mc.yandex.ru/watch/29801259\" style=\"position:absolute; left:-9999px;\" alt=\"\" /></div></noscript><!-- /Yandex.Metrika counter -->";

    @Override
    public void enter(ViewChangeEvent event) {

        usernameField = new TextField("Логин");
        passwordField = new PasswordField("Пароль");
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
        layout.addComponent(passwordField);
        layout.addComponent(loginButton);

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

        Label bbref = new Label("<div align=\"right\">"+YANDEX_METRIC+"</div>", ContentMode.HTML);
        bbref.setWidth(100, Unit.PERCENTAGE);
        //Button bbref = new Button("hhhh");
        //Label spacer = new Label("66");
        //spacer.setWidth(100, Unit.PERCENTAGE);;
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        //horizontalLayout.addComponent(spacer);
        //horizontalLayout.setExpandRatio(spacer, 1);
        horizontalLayout.addComponent(bbref);
        //horizontalLayout.setExpandRatio(bbref, 0);
        horizontalLayout.setComponentAlignment(bbref, Alignment.BOTTOM_RIGHT);
        horizontalLayout.setWidth(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(horizontalLayout);

        setCompositionRoot(verticalLayout);

        //setCompositionRoot(loginLayout);
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