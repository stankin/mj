# Модульный журнал МГТУ Станкин  [![Build Status](https://travis-ci.org/stankin/mj.svg?branch=master)](https://travis-ci.org/stankin/mj)

Веб-интерфейс для просмотра оценок студентами, разрабатываемый для МГТУ Станкин. Также может использоваться как средство OAuth2-аутентификации студентов на внутренних сервисах МГТУ Станкин 
(см [oauthProvider.md](oauthProvider.md)).

Сообщения об ошибках и пожелания вы можете оставить [здесь](https://github.com/stankin/mj/issues).

## Сборка и инсталяция ##

Для сборки из исходников необходим [maven](http://maven.apache.org/) и [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
С более поздними версиями JDK может не собираться из-за [проблем](https://vaadin.com/forum/thread/17976425/can-t-compile-widgetset-vaadin-7-java-12) GWT 2.7 с поддержкой более новых версий Java.

Сборку можно осуществить командой:
```text
mvn clean install -DskipTests
```

Собранное веб приложение будет располагаться по адресу `/target/modules-journal.war` и предназначено для развертывания на сервере приложений [Wildlfy Application Server 10.1.0.Final](http://wildfly.org/), скачать который можно по [ссылке](http://download.jboss.org/wildfly/10.1.0.Final/wildfly-10.1.0.Final.zip).

### Конфигурация

В папке `${jboss.server.config.dir}` (например, `$JBOSS_HOME/standalone/configuration`) должен находиться файл `mj.properties` следующего содержания :

```properties
oauth.google.clientid=клинет_ид_приложения_в_google
oauth.google.secret=секрет_приложения_в_google
oauth.vk.clientid=клинет_ид_приложения_в_vk
oauth.vk.secret=секрет_приложения_в_vk
oauth.yandex.clientid=клинет_ид_приложения_в_yandex
oauth.yandex.secret=секрет_приложения_в_yandex
oauth.callbackurl=http://localhost:8080/mj/callback (или другой при развертывании на сервере)
service.email=почтовый ящик с которого сервер будет отправлять письма
service.recoveryurl=http://localhost:8080/mj/recovery
```


### Postgres

Для работы приложения на сервере должен быть установлен [PostgreSQL](https://www.postgresql.org/).

Сервер WildFly должен иметь поддержку [JDBC-драйвера для postgresql](https://jdbc.postgresql.org/download/postgresql-42.0.0.jar). Для добавления его нужно выполнить следующие команды:

    ./jboss-cli.sh 
    
И внутри него:

    connect
    module add --name=org.postgresql --slot=main --resources=путь-куда-вы-скачали-драйвер --dependencies=javax.api,javax.transaction.api
    /subsystem=datasources/jdbc-driver=postgres:add(driver-name="postgres",driver-module-name="org.postgresql",driver-class-name=org.postgresql.Driver)

Настройки доступа к базе (url, логин, пароль) должны быть указаны в конфигурации WildFly
 (например, `$JBOSS_HOME/standalone/configuration/standalone.xml`) в секции `urn:jboss:domain:datasources:4.0`

```xml
        <subsystem xmlns="urn:jboss:domain:datasources:4.0">
            <datasources>
                <datasource jndi-name="java:jboss/datasources/mj2" jta="false" pool-name="mj-pg-datasource" enabled="true" use-java-context="true">
                    <connection-url>jdbc:postgresql://localhost:5432/mj</connection-url>
                    <driver>postgres</driver>
                    <security>
                        <user-name>login</user-name>
                        <password>password</password>
                    </security>
                </datasource>
                ...
```

### Почта

Для отправки почты почтовый сервер должен быть указан в конфигурации Wildfly:

```xml
<subsystem xmlns="urn:jboss:domain:mail:2.0">
    <mail-session name="default" jndi-name="java:jboss/mail/Default">
        <smtp-server outbound-socket-binding-ref="mail-smtp" ssl="true" username="..." password="..."/>
    </mail-session>
</subsystem>
```
и, например:
```xml
    <socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
        ...
        <outbound-socket-binding name="mail-smtp">
            <remote-destination host="smtp.yandex.ru" port="465"/>
        </outbound-socket-binding>
    </socket-binding-group>
```


### Запуск

Развертывание можно осуществить командой:
```text
mvn wildfly:deploy -DskipTests -Dwildfly.hostname=адрес_сервера
```


### Тестирование ###

Для выполнения тестов (`mvn test`) необходимо предварительно создать в Postgres пользователя `mj_test` с паролем `mj_test` и базу данных `mj_test`.
Сделать это можно (в Ubuntu) командами:

    sudo -u postgres createuser mj_test -d -P
    sudo -u postgres createdb mj_test -E UTF8 -l en_US.UTF-8 -O mj_test

При запросе ввода пароля в качестве пароля введите `mj_test`.

В качестве альтенративы, если вы используете [Docker](https://www.docker.com), то можно использовать подготовленный
в корневом каталоге файл для тестирования `docker-compose-dev.yaml`

    docker-compose -f docker-compose-dev.yaml up


## Работа с приложением ##

Для входа в систему по умолчанию используется логин `admin` и пароль `adminadmin`. Их можно изменить при входе.

Для загруки данных о студентах необходимо сначала загрузить **"эталон"** (пример эталона:     [`src/test/resources/newEtalon.xls`](src/test/resources/newEtalon.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить эталон*, а затем загрузть xls-файлы модульных журналов (пример журнала: [`src/test/resources/information_items_property_2349.xls`](src/test/resources/information_items_property_2349.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить файлы с оценками*, или перетащить их из файлового менеджера в на поле *Перетащите файлы*.

После этого в таблице слева окажется список студентов, и, если кликнуть на студента в списке, то справа будет отображены его текущие оценки.

Также доступен поиск по имени студента и группе.

Кликнув по кнопке *Аккаунт* в правом верхнем углу экрана можно отредактировать пароль администратора.

Кликнув по кнопке *Редактировать* рядом с фамилией студента (становится активной после выбора студента) можно отредактировать данные для аутентификации студентов.
