package ru.stankin.mj.model
import kotlinx.support.jdk7.use
import org.sql2o.Sql2o
import ru.stankin.mj.utils.ThreadLocalTransaction
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by nickl on 09.03.17.
 */
@Singleton
class ModulesJournalXMLUploader {
    @Inject
    private lateinit var storage: StudentsStorage
    @Inject
    private lateinit var modules: ModulesStorage
    @Inject
    private lateinit var sql2o: Sql2o
    fun updateFromXml(semestr: String, inputStream: InputStream): List<String> {
        val messages = ArrayList<String>()
        val modulesUpdateStat = ModulesUpdateStat(0, 0, 0)
        ThreadLocalTransaction.joinOrNew(sql2o) {
            val processedCards = HashSet<String>()
            StudentsXML.readStudentsFromXML(inputStream).use {
                it.forEach { xmlStudent ->
                    val student = mergeWithExisting(xmlStudent)
                    processedCards.add(student.cardid!!)
                    storage.saveStudent(student, semestr)
                    messages.addAll(checkModules(student))
                    if (student.modules.isNotEmpty())
                        modulesUpdateStat += modules.updateModules(student)
                    else
                        messages.add("Студент ${student.cardid} ${student.stgroup} ${student.surname} не имеет модулей")
                }
            }
            val forgottenStudents = storage.students.use {
                it.filter { !processedCards.contains(it.cardid) }.count()
            }
            messages.add(0, "В базе присутствует $forgottenStudents студентов, которых нет в загруженном файле")
        }
        messages.add(0, "Модулей: добавлено: " + modulesUpdateStat.added +
                ", обновлено: " + modulesUpdateStat.updated +
                ", удалено: " + modulesUpdateStat.deleted)
        return messages
    }
    private fun mergeWithExisting(excelStudent: Student): Student {
        var student = storage.getStudentByCardId(excelStudent.cardid!!)
        if (student == null)
            student = excelStudent
        else {
            student.surname = excelStudent.surname
            student.name = excelStudent.name
            student.patronym = excelStudent.patronym
            student.cardid = excelStudent.cardid
            student.stgroup = excelStudent.stgroup
            student.initials = null
            //logger.debug("initi student {}", student);
            student.initialsFromNP()
            student.modules = excelStudent.modules
        }
        return student
    }
    private fun checkModules(student: Student): List<String> {
        val messages = ArrayList<String>()
        for (module in student.modules) {
            if (module.color == Module.YELLOW_MODULE && module.value > 25) {
                messages.add("Просроченный модуль > 25 :"
                        + student.stgroup + " "
                        + student.surname + " "
                        + student.initials + " "
                        + module.subject.title + ": "
                        + module.value
                )
            }
        }
        return messages;
    }
}