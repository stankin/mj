# Модульный журнал МГТУ Станкин #

Веб-интерфейс для просмотра оценок студентами, разрабатываемый для МГТУ Станкин.

Сообщения об ошибках и пожелания вы можете оставить [здесь](https://bitbucket.org/NicolayMitropolsky/stankin-mj/issues?sort=status).

## Сборка и инсталяция ##

Для сборки из исходников необходим [maven](http://maven.apache.org/) и [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Сборку можно осуществить командой:
```text
mvn clean install -DskipTests
```

Собранное веб приложение будет располагаться по адресу `/target/modules-journal.war` и предназначено для развертывания на сервере приложений [Wildlfy Application Server 9.0.1.Final](http://wildfly.org/), скачать который можно по [ссылке](http://download.jboss.org/wildfly/9.0.1.Final/wildfly-9.0.1.Final.zip).

При первом запуске, для автоматического создания схемы в базе данных необходимо установить в файле
`src/main/resources/META-INF/persistence.xml` значение `hibernate.hbm2ddl.auto` равным `update`:

```xml
    <property name="hibernate.hbm2ddl.auto" value="update"/>
```

Так же его имеет смысл сохранить равным `update` если планируется изменять схему базы данных



### Тестирование ###

Для выполнения тестов (`mvn test`) необходимо указать в переменной окружения `JBOSS_HOME` путь к установленному серверу Wildfly.

### Known issues ###

В некоторых случаях нужно указать имя файла базы данных без специальных символов в [`src/main/webapp/WEB-INF/mj2-ds.xml:29`](src/main/webapp/WEB-INF/mj2-ds.xml?fileviewer=file-view-default#mj2-ds.xml-29), заменив `~/test:mj2` на какой-нибудь другой путь на вашем диске.

### H2 fix ###

Wildfly использует встроенную базу данных **H2** версии `1.3.173`, которая подвержена багу c логированием `isWrapperFor` значительно снижащим производетельность. 

Следует либо обновить **H2** до версии к примеру `1.4.188` заменив jar-файл в Wildfly по адресу: `modules/system/layers/base/com/h2database/h2/main` и обновивив версию в соответствующем `module.xml`.

Либо отключить логирование как советуют [здесь](https://github.com/rundeck/rundeck/issues/1175)


## Работа с приложением ##

Для входа в систему по умолчанию используется логин `admin` и пароль `adminadmin`. Их можно изменить при входе.

Для загруки данных о студентах необходимо сначала загрузить **"эталон"** (пример эталона:     [`src/test/resources/newEtalon.xls`](src/test/resources/newEtalon.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить эталон*, а затем загрузть xls-файлы модульных журналов (пример журнала: [`src/test/resources/information_items_property_2349.xls`](src/test/resources/information_items_property_2349.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить файлы с оценками*, или перетащить их из файлового менеджера в на поле *Перетащите файлы*.

После этого в таблице слева окажется список студентов, и, если кликнуть на студента в списке, то справа будет отображены его текущие оценки.

Также доступен поиск по имени студента и группе.

Кликнув по кнопке *Аккаунт* в правом верхнем углу экрана можно отредактировать пароль администратора.

Кликнув по кнопке *Редактировать* рядом с фамилией студента (становится активной после выбора студента) можно отредактировать данные для аутентификации студентов.