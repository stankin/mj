import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.stankin.mj.model.Student

Map<Student, Student> studentMap = new HashMap<>(3000)

def jsonSlurper = new JsonSlurper()



def additionalGenerated = new File("additionalUsers.json").collect { line -> jsonSlurper.parseText(line) }.listIterator()

private Student readStudent(json) {
    new Student(json.group.trim(), json.surname.trim(), Student.initialsFromNames(json.name, json.patronum).trim())
}

new File("conversion.jsons").eachLine {
    def conv = jsonSlurper.parseText(it)
    def student = readStudent(conv.old)
    println("read student: " + student)
    studentMap[student] = readStudent(conv.new)
}

new File("src/test/resources/Модули").eachFileMatch(FileType.FILES, ~/information_items_property_\d+.xls/, { file ->
    println("file: " + file)

    InputStream inputStream = new FileInputStream(file)
    inputStream.withCloseable {
        Workbook workbook = WorkbookFactory.create(inputStream);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);


            if (sheet.getPhysicalNumberOfRows() > 4) {

                sheet.iterator().forEachRemaining { row ->
                    if (row.getCell(0) != null && !row.getCell(0).getStringCellValue().isEmpty()) {

                        String group = row.getCell(0).getStringCellValue().trim();
                        String surname = row.getCell(1).getStringCellValue().trim();
                        String initials = row.getCell(2).getStringCellValue().trim();

                        Student oldStudent = new Student(group, surname, initials.replaceAll(/\.\s*\./, "."))

                        Student student = studentMap[oldStudent]
                        if (student == null) {
                            println("oldStudent: " + oldStudent)

                            def generated = additionalGenerated.next()

                            student = genAsStudent(generated, oldStudent.stgroup)

                            println("mapped to: " + student)
                        }

                        row.getCell(0).setCellValue(student.stgroup)
                        row.getCell(1).setCellValue(student.surname)
                        row.getCell(2).setCellValue(student.initials)


                    }
                }
            }
        }


        def outputStream = new FileOutputStream("src/test/resources/" + file.getName())
        workbook.write(outputStream)
        outputStream.close()


    }

})

public static Student genAsStudent(generated, String stgroup) {
    String newSurname = generated.lname as String
    String newName = generated.fname as String
    String newPatronum = generated.patronymic as String
    Integer newCardId = generated.carid as Integer


    return new Student(stgroup, newSurname.trim(), Student.initialsFromNames(newName, newPatronum).trim())
}


