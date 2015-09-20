# Различные скрипты #

Вспомогательные скрипты для работы проекта, используемые для
миграции данных и тому подобного.

Запускать скрипты можно командой из родительского каталога:

`mvn org.codehaus.gmaven:groovy-maven-plugin:2.0:execute -Dsource=scripts/GenerateTestData.groovy  -Dscope=test -Dscriptpath=./scripts/`