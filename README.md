# Модульный журнал МГТУ Станкин #

Веб-интерфейс для просмотра оценок студентами, разрабатываемый для МГТУ Станкин.

Сообщения об ошибках и пожелания вы можете оставить [здесь](https://bitbucket.org/NicolayMitropolsky/stankin-mj/issues?status=new&status=open).

## Установка ##

Для сборки из исходников необходим [maven](http://maven.apache.org/) и [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Сборку можно осуществить командой:
```text
mvn clean install
```

Собранное веб приложение будет располагаться по адресу `/target/modules-journal.war` и предназначено для развертывания на сервере приложений [Wildlfy Application Server	
8.1.0.Final](http://wildfly.org/), скачать который можно по [ссылке](http://download.jboss.org/wildfly/8.1.0.Final/wildfly-8.1.0.Final.zip).

## Работа с приложением ##

Для загруки данных о студентах необходимо сначала загрузить **"эталон"**, кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить эталон*, а затем загрузть xls-файлы модульных журналов, кликнув по кнопке *Выбрать Файлы* под надписью *Загрузить файлы с оценками*, или перетащить их из файлового менеджера в на поле *Перетащите файлы*.

После этого в таблице слева окажется список студентов, и, если кликнуть на студента в списке, то справа будет отображены его текущие оценки.

Также доступен поиск по имени студента и группе.

Кликнув по кнопке *Аккаунт* в правом верхнем углу экрана можно отредактировать пароль администратора.

Кликнув по кнопке *Редактировать* рядом с фамилией студента (становится активной после выбора студента) можно отредактировать данные для авторизации студентов.