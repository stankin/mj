package ru.stankin.mj


import io.kotlintest.matchers.be
import kotlinx.support.jdk7.use
import kotlinx.support.jdk8.streams.toList
import org.sql2o.Sql2o
import ru.stankin.mj.model.*
import ru.stankin.mj.model.user.AdminUser
import ru.stankin.mj.testutils.InWeldTest
import ru.stankin.mj.testutils.asAdminTransaction
import ru.stankin.mj.utils.ThreadLocalTransaction


/**
 * Created by nickl on 08.04.17.
 */
class ModulesNotificatorTest  : InWeldTest() {
    init {


        test("Should notify on new modules") {

            val studentBase = Student("1")
            bean<StudentsStorage>().saveStudent(studentBase, "")


            val module1 = Module(Subject("", "1", "1", 1.0), "M1")
            val module2 = Module(Subject("", "1", "1", 1.0), "M2")
            val firtsTrModules = listOf(
                    module1.clone().apply { value = 0 },
                    module2.clone().apply { value = 0 }
            )

            val student1 = studentBase.apply { modules = firtsTrModules }

            asAdminTransaction {
                bean<ModulesStorage>().updateModules(student1)
            }

            val studentModules = bean<ModulesStorage>().getStudentModules("", student1.id)
            println("studentModules = " + studentModules)
            studentModules shouldBe firtsTrModules

            val secondTrModules = listOf(
                    module1.clone().apply { value = 40 },
                    module2.clone().apply { value = 45 }
            )
            val student2 = studentBase.apply { modules = secondTrModules }

            asAdminTransaction {
                bean<ModulesStorage>().updateModules(student2)
            }

            val studentModules2 = bean<ModulesStorage>().getStudentModules("", student2.id)
            println("studentModules = " + studentModules2)
            studentModules2 shouldBe secondTrModules

            bean<ModulesStorage>().getStudentModulesChanges("", student2.id).values.toSet() shouldEqual setOf(
                    firtsTrModules, secondTrModules
            )

        }

    }


}