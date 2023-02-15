package ru.stankin.mj

import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.ModulesStorage
import ru.stankin.mj.model.Student
import ru.stankin.mj.model.StudentsStorage
import ru.stankin.mj.model.UserResolver
import ru.stankin.mj.oauthprovider.OAuthProvider
import ru.stankin.mj.rested.StudentApi
import ru.stankin.mj.rested.security.ShiroListener
import ru.stankin.mj.testutils.InWeldWebTest
import ru.stankin.mj.testutils.MockableShiroFilter
import java.net.HttpURLConnection
import javax.ws.rs.core.HttpHeaders

class StudentsApiTest : InWeldWebTest() {


    private val log = LogManager.getLogger(StudentsApiTest::class.java)

    override fun restClasses() = listOf(StudentApi::class.java)
    override fun filters() = listOf(filter<MockableShiroFilter>("/*"))
    override fun listeners() = listOf(listener<ShiroListener>())
    override val restApiPrefix: String get() = "webapi"

    init {

        test("api student") {

            val student = getStudentMarks("/information_items_property_2349.xls", "sem1").toList().get(35)
            asAdminTransaction {
                bean<StudentsStorage>().saveStudent(student, "sem1")
                bean<ModulesStorage>().updateModules(student)
            }
            val token = makeStudentToken(student)
            (restURL("/api3/student/semesters")
                .openConnection() as HttpURLConnection).let { connection ->
                connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer $token")
                connection.responseCode shouldBe 200
                connection.inputStream.use { it.reader().readText() } shouldBe "[\"sem1\"]"
            }  
            (restURL("/api3/student/marks?sem=sem1")
                .openConnection() as HttpURLConnection).let { connection ->
                connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer $token")
                connection.responseCode shouldBe 200
                connection.inputStream.use { it.reader().readText() } shouldBe "{\"group\":\"ИДБ-13-12\",\"name\":null,\"surname\":\"Савельева\",\"patronym\":null,\"initials\":\"С.А.\",\"modules\":[{\"subject\":{\"title\":\"Архитектура ЭВМ и систем\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":45,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1},\"exam\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Высокоэф. технол. и обор. Современных производст\",\"factor\":0.0},\"marks\":{\"credit\":{\"value\":0,\"color\":-1},\"module1\":{\"value\":0,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Иностранный язык\",\"factor\":0.0},\"marks\":{\"credit\":{\"value\":0,\"color\":-1},\"module1\":{\"value\":48,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Компьютерная графика\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":45,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1},\"exam\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Математическая логика и теория алгоритмов\",\"factor\":0.0},\"marks\":{\"credit\":{\"value\":0,\"color\":-1},\"module1\":{\"value\":53,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Политология\",\"factor\":0.0},\"marks\":{\"credit\":{\"value\":0,\"color\":-1},\"module1\":{\"value\":32,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Технологии программирования\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":50,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1},\"exam\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Физика\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":35,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1},\"exam\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"ФК\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":38,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Философия\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":46,\"color\":-1},\"module2\":{\"value\":0,\"color\":-1},\"exam\":{\"value\":0,\"color\":-1}}},{\"subject\":{\"title\":\"Рейтинг\",\"factor\":0.0},\"marks\":{\"module1\":{\"value\":11,\"color\":-1}}}]}"
                        
            }
        }

        test("api student wrong bearer") {
            val student = Student("OAuthStudent2", "1", "2", "3", "4")
            bean<UserResolver>().saveUser(student)
            val connection = restURL("/api3/student/marks")
                .openConnection() as HttpURLConnection
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer 00000")
            connection.responseCode shouldBe 401
        }

        test("api student unauthorised") {
            val connection = restURL("/api3/student/marks?sem=1").openConnection() as HttpURLConnection
            connection.responseCode shouldBe 401
        }

    }

    private fun makeStudentToken(student: Student): String {
        bean<UserResolver>().saveUser(student)
        val provider = bean<OAuthProvider>()
        val (clientId, secret) = provider.registerConsumer("testService", "some@some.com", emptyList())
        val permission = provider.addUserPermission(clientId, student.id.toLong())
        return permission
    }
}


