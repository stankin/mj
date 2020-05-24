# Модульный журнал МГТУ Станкин  [![Build Status](https://travis-ci.org/stankin/mj.svg?branch=master)](https://travis-ci.org/stankin/mj)

Веб-интерфейс для просмотра оценок студентами, разрабатываемый для МГТУ Станкин. Также может использоваться как средство OAuth2-аутентификации студентов на внутренних сервисах МГТУ Станкин 
(см [oauthProvider.md](oauthProvider.md)).

Сообщения об ошибках и пожелания вы можете оставить [здесь](https://github.com/stankin/mj/issues).

## Сборка и инсталяция ##

Для сборки из исходников необходим [maven](http://maven.apache.org/) и [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
С более поздними версиями JDK может не собираться из-за [проблем](https://vaadin.com/forum/thread/17976425/can-t-compile-widgetset-vaadin-7-java-12) GWT 2.7 с поддержкой более новых версий Java.

При этом работа приложения может осуществляться и на более поздних версиях JDK (По крайней мере в настойщий момент приложений работает на JDK 11)

Сборку можно осуществить командой:
```text
mvn clean install -DskipTests
```

Собранное веб приложение будет располагаться по адресу `/target/modules-journal.war` и предназначено для развертывания на сервере приложений [Wildlfy Application Server 19.0.0.Final](http://wildfly.org/), скачать который можно по [ссылке](https://download.jboss.org/wildfly/19.0.0.Final/wildfly-19.0.0.Final.zip).

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

Сервер WildFly должен иметь поддержку [JDBC-драйвера для postgresql](https://jdbc.postgresql.org/download/postgresql-42.2.12.jar). Для добавления его нужно выполнить следующие команды:

    wget https://jdbc.postgresql.org/download/postgresql-42.2.12.jar
    ./jboss-cli.sh 
    
И внутри него:

    connect
    module add --name=org.postgresql --slot=main --resources=путь-куда-вы-скачали-драйвер --dependencies=javax.api,javax.transaction.api
    /subsystem=datasources/jdbc-driver=postgres:add(driver-name="postgres",driver-module-name="org.postgresql",driver-class-name=org.postgresql.Driver)

Настройки доступа к базе (url, логин, пароль) должны быть указаны в конфигурации WildFly

    data-source add --jndi-name=java:jboss/datasources/mj2 --name=mj --connection-url=jdbc:postgresql://localhost:5432/mj --driver-name=postgres --user-name=login --password=password
    

### Почта

Для отправки почты почтовый сервер должен быть указан в конфигурации Wildfly, можно добавить через `jboss-cli`:

    batch
    /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=username,value=ВАШ-EMAIL)
    /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=password,value=ВАШ-ПАРОЛЬ)
    /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=ssl,value=true)
    /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp/:write-attribute(name=host,value=smtp.yandex.ru)
    /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp/:write-attribute(name=port,value=465)
    run-batch

### Размер загружаемых файлов

По-умолчанию размер загружаемых файлов на Wildfly ограничен 10 мегабайтами, что меньше чем размер загружаемого на 2020 год выгрузки из 1С.
Этот размер можно увеличить до 64 Мб командой в `jboss-cli.sh`

    /subsystem=undertow/server=default-server/http-listener=default/:write-attribute(name=max-post-size,value=67108864)
    
Также имеет смысл увеличить максимальное время соединения для загрузки больших файлов

    /subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=no-request-timeout, value=600000)
    
Если в качестве фронтента выступает nginx то ему в соответствующий блок `server` аналогично нужно добавить:

    client_max_body_size 64m;
    
А в настройки proxy:

    proxy_connect_timeout 1200s; 
    proxy_read_timeout 1200s;

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

В качестве альтернативы, если вы используете [Docker](https://www.docker.com), то можно использовать подготовленный
в корневом каталоге файл для тестирования `docker-compose-dev.yaml`

    docker-compose -f docker-compose-dev.yaml up


## Работа с приложением ##

Для входа в систему по умолчанию используется логин `admin` и пароль `adminadmin`. Их можно изменить при входе.

Для загруки данных о студентах необходимо сначала загрузить **"эталон"** (пример эталона:     [`src/test/resources/newEtalon.xls`](src/test/resources/newEtalon.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить эталон*, а затем загрузть xls-файлы модульных журналов (пример журнала: [`src/test/resources/information_items_property_2349.xls`](src/test/resources/information_items_property_2349.xls)), кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить файлы с оценками*, или перетащить их из файлового менеджера в на поле *Перетащите файлы*.

После этого в таблице слева окажется список студентов, и, если кликнуть на студента в списке, то справа будет отображены его текущие оценки.

Также доступен поиск по имени студента и группе.

Кликнув по кнопке *Аккаунт* в правом верхнем углу экрана можно отредактировать пароль администратора.

Кликнув по кнопке *Редактировать* рядом с фамилией студента (становится активной после выбора студента) можно отредактировать данные для аутентификации студентов.
