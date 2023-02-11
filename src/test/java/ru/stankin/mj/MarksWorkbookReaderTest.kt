package ru.stankin.mj

import io.kotlintest.specs.FunSpec
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
    

}

fun getStudentMarks(name: String, sem: String = ""): Collection<Student> {

    return  MarksWorkbookReader.modulesFromExcel(MarksWorkbookReaderTest::class.java.getResourceAsStream(name), sem)
        .collect(Collectors.toList<Student>())

}
