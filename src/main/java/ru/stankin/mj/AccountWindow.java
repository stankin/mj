package ru.stankin.mj;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.*;
import ru.stankin.mj.model.Student;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by nickl on 16.02.15.
 */
public class AccountWindow extends Window {

    User user;

    Consumer<User> save;

    public AccountWindow(User user, Consumer<User> save) {
        super("Аккаунт: "+user.getUsername());
        this.user = user;
        this.save = save;
        this.setModal(true);
        this.setDraggable(false);
        this.setResizable(false);
        VerticalLayout vertical = new VerticalLayout();
        vertical.setMargin(true);
        final FormLayout content = new FormLayout();
        final FieldGroup binder = new FieldGroup(new BeanItem<>(user));
        content.addComponent(readOnlyField("Логин:", user.getUsername()));
        //content.addComponent(new TextField("Пароль:", new ));
        content.addComponent(binder.buildAndBind("Пароль:", "password"));
        if(user instanceof Student){
            Student student = (Student) user;
            content.addComponent(readOnlyField("Группа:", student.stgroup));
            content.addComponent(readOnlyField("Фамилия:", student.surname));
            content.addComponent(readOnlyField("Имя:", student.name));
            content.addComponent(readOnlyField("Отчество:", student.patronym));
            content.addComponent(readOnlyField("Инициалы:", student.initials));
        }
        vertical.addComponent(content);
        vertical.addComponent(
                new HorizontalLayout(
                        new Button("Сохранить", e -> {
                            try {
                                binder.commit();
                            } catch (FieldGroup.CommitException e1) {
                                e1.printStackTrace();
                            }
                            this.save.accept(user);
                            this.close();
                        }),
                        new Button("Отменить", event -> this.close()))
        );

        this.setContent(vertical);
        this.center();
    }

    private TextField readOnlyField(String caption, String value) {
        TextField filed = new TextField(caption, value);
        filed.setReadOnly(true);
        return filed;
    }


}
