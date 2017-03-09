package ru.stankin.mj

import io.kotlintest.specs.FunSpec
import kotlinx.support.jdk7.use
import kotlinx.support.jdk8.collections.stream
import kotlinx.support.jdk8.streams.toList
import org.apache.logging.log4j.LogManager
import org.intellij.lang.annotations.Language
import ru.stankin.mj.model.*
import ru.stankin.mj.utils.stream
import java.io.ByteArrayInputStream


/**
 * Created by nickl on 09.03.17.
 */


class ModulesJournalXMLParserTest : FunSpec() {

    private val log = LogManager.getLogger(ModulesJournalXMLParserTest::class.java)

    init {

        test("StudentsXML") {


            val stre = StudentsXML.readStudentsFromXML(ByteArrayInputStream("""
<sem>
<group name="ИВБ-12-01">
		<student surame="Попов" firstname="Андрей" patronymic="Васильевич" stud_id="123456" status="1" rating="0" accumRating="0" oldAccumRating="0" numSemester="0">
			<discipline name="Безопасность жизнедеятельности" factor="0">
				<exam type="5" mark="25" P="0" status="0"/>
			</discipline>
			<discipline rating="0.0000000000" name="Методы и средства измерений, испытаний и контроля" factor="0">
				<exam type="3" mark="50" P="0" status="0"/>
				<exam type="4" mark="0" P="0" status="0"/>
			</discipline>
		</student>
		<student surame="Васильвева" firstname="Варавара" patronymic="Вячеслвана" stud_id="7894561" status="1" rating="0" accumRating="0" oldAccumRating="0" numSemester="0">
			<discipline name="Безопасность жизнедеятельности" factor="0">
				<exam type="5" mark="30" P="0" status="0"/>
			</discipline>
			<discipline rating="0.0000000000" name="Методы и средства измерений, испытаний и контроля" factor="0">
				<exam type="3" mark="45" P="0" status="0"/>
				<exam type="4" mark="0" P="0" status="0"/>
			</discipline>
		</student>
</group>
<group name="ИВБ-12-02">
		<student surame="Красниченко" firstname="Денис" patronymic="Анатольевич" stud_id="123457" status="1" rating="0" accumRating="0" oldAccumRating="0" numSemester="0">
			<discipline name="Безопасность жизнедеятельности" factor="0">
				<exam type="5" mark="25" P="0" status="0"/>
			</discipline>
			<discipline rating="0.0000000000" name="Методы и средства измерений, испытаний и контроля" factor="0">
				<exam type="3" mark="30" P="0" status="0"/>
				<exam type="4" mark="0" P="0" status="0"/>
			</discipline>
		</student>
</group>
</sem>
""".toByteArray()), "testSemester")

            val students = stre.use { it.toList() }

            students.count() shouldBe 3
            students.map { it.modules.size }.sum() shouldBe 9

            students[0].surname shouldBe "Попов"
            students[0].surname shouldBe "Попов"

            students[0].modules[1].subject.title shouldBe "Методы и средства измерений, испытаний и контроля"
            students[0].modules[1].value shouldBe 50

        }

    }


}