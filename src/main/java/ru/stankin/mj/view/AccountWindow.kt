package ru.stankin.mj.view

import com.vaadin.data.Validatable
import com.vaadin.data.fieldgroup.FieldGroup
import com.vaadin.data.util.BeanItem
import com.vaadin.data.validator.AbstractStringValidator
import com.vaadin.server.UserError
import com.vaadin.ui.*
import ru.stankin.mj.model.AuthenticationsStore
import ru.stankin.mj.model.user.User
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.UserResolver

import java.util.function.BiConsumer

/**
 * Created by nickl on 16.02.15.
 */
class AccountWindow @JvmOverloads constructor(
        internal var user: User,
        internal var userDAO: UserResolver,
        internal var auth: AuthenticationsStore,
        adminMode: Boolean = false, requireOldPassword: Boolean = !adminMode) : Window("Аккаунт: " + user.username) {

    init {
        this.isModal = true
        this.isDraggable = false
        this.isResizable = false
        val vertical = VerticalLayout()
        vertical.setMargin(true)
        val warnPassword = !adminMode && auth.acceptPassword(user.id, user.username)
        if (warnPassword) {
            vertical.addComponent(Label("ВНИМАНИЕ!"))
            vertical.addComponent(Label("На вашем аккаунте используется пароль по умолчанию, пожалуйста, смените его!"))
        }
        if(!adminMode && user.email.isNullOrEmpty())
            vertical.addComponent(Label("На вашем аккаунте не указан адрес электроной почты, пожалуйста, укажите адрес, " +
                    "по которому можно с вами связаться!"))

        val content = FormLayout()
        val binder = FieldGroup(BeanItem(user))
        content.addComponent(readOnlyField("Логин:", user.username))
        if (user is Student) {
            val student = user as Student
            content.addComponent(readOnlyField("Группа:", student.stgroup))
            content.addComponent(readOnlyField("Фамилия:", student.surname))
            content.addComponent(readOnlyField("Имя:", student.name))
            content.addComponent(readOnlyField("Отчество:", student.patronym))
            content.addComponent(readOnlyField("Инициалы:", student.initials))
        }
        val email = (binder.buildAndBind("email", "email") as TextField).apply {
            description = "Укажите адрес электронной почты, он пригодится если вы забудете пароль."
        }
        if(user.email == null)
            email.value = ""

        content.addComponent(email)
        //content.addComponent(new TextField("Пароль:", new ));


        val password = (if(adminMode) TextField("Новый пароль:", "") else PasswordField("Новый пароль:", "")).apply {
            description = "Оставьте поле пустым если не хотите менять пароль"
            addValidator(object : AbstractStringValidator("Пароль не должен совпадать с логином") {
                override fun isValidValue(value: String): Boolean {
                    return adminMode || value != user.username
                }
            })
        }

        val removeAuths = CheckBox("Сбросить вход через внешние сервисы")

        val oldPassword = if (requireOldPassword) PasswordField("Текущий Пароль:", "").apply {
            description = "Укажите текущий пароль к вашему аккаунту"
            addValidator(object : AbstractStringValidator("Необходимо указать верный пароль") {
                override fun isValidValue(value: String): Boolean = when {
                    password.value.isEmpty() && !removeAuths.value && !email.isModified-> true
                    else -> auth.acceptPassword(user.id, value)
                }
            })
        } else null


        oldPassword?.let {
            content.addComponent(it)
        }

        content.addComponent(password)
        content.addComponent(removeAuths)
        vertical.addComponent(content)
        vertical.addComponent(
                HorizontalLayout(
                        Button("Сохранить") { e ->
                            oldPassword?.markAsDirty()
                            if (binder.isValid && content.mapNotNull { it as? Validatable }.all { it.isValid }) {
                                try {
                                    binder.commit()
                                } catch (e1: FieldGroup.CommitException) {
                                    e1.printStackTrace()
                                }

                                this.userDAO.saveUserAndPassword(user, password.value)
                                if(removeAuths.value){
                                    auth.dropExternalAuths(user.id)
                                }
                                this.close()
                            }
                        },
                        Button("Отменить") { event -> this.close() })
        )

        this.content = vertical
        this.center()
    }


    private fun readOnlyField(caption: String, value: String): TextField {
        val filed = TextField(caption, value)
        filed.isReadOnly = true
        return filed
    }


}
