package ru.stankin.mj

import io.kotlintest.specs.FunSpec
import kotlinx.support.jdk7.use
import kotlinx.support.jdk8.collections.stream
import ru.stankin.mj.model.*

import java.util.stream.Collectors


class MarksWorkbookReaderTest : FunSpec() {

    init {

        test("testProcessIncomingDate1") {

            val students = getStudentMarks("/information_items_property_2349.xls")
            students.size.toLong() shouldBe 133L
            val collect = students.stream().mapToInt({ s -> s.getModules().size }).sum()
            collect.toInt().toLong() shouldBe 4067L

        }


        test("testProcessIncomingData4withBlack") {
            val students = getStudentMarks("/2 курс II семестр 2014-2015.xls")

            students.size.toLong() shouldBe 119L

            val pletenev = students.stream().filter({ s: Student -> s.cardid == "2222222" }).findAny().get()

            println("pletenev = " + pletenev)

            pletenev.getModules().stream()
                    .filter({ m -> m.getSubject().getTitle() == "Правоведение" })
                    .filter({ m -> m.getValue() > 0 })
                    .count() shouldBe 0L
            pletenev.getModules().stream()
                    .filter({ m -> m.getSubject().getTitle() == "Операционные системы" })
                    .filter({ m -> m.getValue() > 0 })
                    .count() shouldBe 3L

            students.stream().mapToInt({ s -> s.getModules().size }).sum().toLong() shouldBe 3751L
        }

        test("testLoadStudentsList") {

            val students = MarksWorkbookReader.updateStudentsFromExcel(
                    MarksWorkbookReaderTest::class.java.getResourceAsStream("/newEtalon.xls")
            )

            students.use { students -> students.count() shouldBe 1753L }
        }

    }

    private fun getStudentMarks(name: String): Collection<Student> {

       return  MarksWorkbookReader.modulesFromExcel(MarksWorkbookReaderTest::class.java.getResourceAsStream(name), "")
               .collect(Collectors.toList<Student>())

    }

}
