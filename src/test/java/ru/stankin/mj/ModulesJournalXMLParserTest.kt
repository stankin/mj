package ru.stankin.mj

import io.kotlintest.matchers.be
import io.kotlintest.specs.FunSpec
import org.apache.logging.log4j.LogManager
import ru.stankin.mj.model.MarksWorkbookReader
import ru.stankin.mj.model.StudentsXML
import ru.stankin.mj.testutils.Matchers.ne
import java.io.ByteArrayInputStream
import kotlin.streams.toList


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
""".toByteArray()))

            val students = stre.use { it.toList() }

            students.count() shouldBe 3
            students.map { it.modules.size }.sum() shouldBe 15

            students[0].surname shouldBe "Попов"

            students[0].modules[1].subject.title shouldBe "Методы и средства измерений, испытаний и контроля"
            students[0].modules[1].value shouldBe 50
            println("students modules:" + students[0].modules)

        }

        test("StudentsXML2") {


            val stre = StudentsXML.readStudentsFromXML(ByteArrayInputStream("""<?xml version="1.0" encoding="UTF-8"?>
<sem date="28.03.2017.07.09.35" title="2016-весна">
	<group name="ИДБ-13-01">
		<student surame="Сергеев" firstname="Владимир" patronymic="Петрович" stud_id="121212" status="1" rating="44,26917" accumRating="36,06701" oldAccumRating="34,89527" numSemester="0">
			<discipline name="Защита выпускной квалификационной работы" factor="0">
				<exam type="" mark="0" P="0" status="0"/>
			</discipline>
			<discipline name="Вычислительные машины, системы и сети" factor="3,5">
				<exam type="5" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="40" P="0" status="0"/>
			</discipline>
			<discipline name="Диагностика и надежность  автоматизированных систем" factor="3">
				<exam type="3" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="0" P="0" status="0"/>
			</discipline>
</student>
		<student surame="Ходько" firstname="Александр" patronymic="Алексеевич" stud_id="113114" status="1" rating="0" accumRating="0" oldAccumRating="0" numSemester="0">
			<discipline name="Защита выпускной квалификационной работы" factor="0">
				<exam type="" mark="0" P="0" status="0"/>
			</discipline>
			<discipline name="Вычислительные машины, системы и сети" factor="3,5">
				<exam type="5" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="40" P="0" status="0"/>
			</discipline>
		</student>
</group>
<group name="ИДБ-13-05">
		<student surame="Мергаева" firstname="Анастасия" patronymic="Андреевна" stud_id="113115" status="1" rating="0" accumRating="0" oldAccumRating="0" numSemester="0">
			<discipline name="Защита выпускной квалификационной работы" factor="0">
				<exam type="" mark="0" P="0" status="0"/>
			</discipline>
			<discipline name="Программное обеспечение мехатронных и робототехнических систем" factor="2,5">
				<exam type="5" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="0" P="0" status="0"/>
			</discipline>
			<discipline name="Моделирование и исследование робототехнических систем" factor="3">
				<exam type="3" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="0" P="0" status="0"/>
			</discipline>
		</student>
		<student surame="Копытов" firstname="Владимир" patronymic="Олегович" stud_id="113116" status="1" rating="44,26917" accumRating="36,06701" oldAccumRating="34,89527" numSemester="0">
			<discipline name="Защита выпускной квалификационной работы" factor="0">
				<exam type="" mark="0" P="0" status="0"/>
			</discipline>
			<discipline name="Компьютерное управление мехатронными системами" factor="2,5">
				<exam type="4" mark="0" P="0" status="0"/>
				<exam type="5" mark="0" P="0" status="0"/>
				<exam type="2" mark="0" P="0" status="0"/>
				<exam type="1" mark="50" P="0" status="0"/>
			</discipline>
		</student></group>
</sem>
""".toByteArray()))

            val students = stre.use { it.toList() }

            students.count() shouldBe 4
            students.map { it.modules.size }.sum() shouldBe 31

            students[0].surname shouldBe "Сергеев"

            students[0].modules[1].subject.title shouldBe "Вычислительные машины, системы и сети"
            students[0].modules[1].value shouldBe 0
            students[0].modules[1].subject.semester shouldBe "2016-весна"
            val ratingModule = students[0].modules.find { it.subject.title == MarksWorkbookReader.ACCOUMULATED_RATING }
            ratingModule should be ne null
            ratingModule!!.num shouldBe "М1"
            ratingModule!!.value shouldBe 36
        }


    }


}