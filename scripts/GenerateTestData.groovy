

import groovy.json.JsonSlurper
import groovy.transform.Field
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static ru.stankin.mj.model.ModuleJournalUploader.stream
import static ru.stankin.mj.model.ModuleJournalUploader.stringValue



println("Hello")

def jsonSlurper = new JsonSlurper()

File theInfoFile = new File("users.json")

@Field DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")


def generatedUsers = theInfoFile.collect {line -> jsonSlurper.parseText(line)}.listIterator()

//r.each {println(it.fname)}


Workbook workbook = WorkbookFactory.create(
        new File("src/test/resources/Эталон на 21.10.2014.xls")
);

Sheet sheet = workbook.getSheetAt(0)

println(sheet.sheetName)

class ETALON_CELLS{
  public static final int SURNAME = 0
  public static final int NAME = 1
  public static final int PATRONUM = 2
  public static final int CARDID = 3
  public static final int GROUP = 5
  public static final int DORMITORY  = 6
  public static final int EDUCATIONFORM  = 7
  public static final int HOUSE_PHONE  = 11
  public static final int MOBILE_PHONE  = 12
  public static final int GENDER  = 13
  public static final int F_LANGUAGE  = 14
  public static final int BIRTH_DATE  = 15
  public static final int NATIONALITY  = 16
  public static final int AREA  = 17
  public static final int EDU_FORM  = 18
  public static final int PRIVILEGIA  = 19
}

public static Date asDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
}
public static Date asDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
}

stream(sheet.rowIterator())
        .skip(1)
        .filter{it.getCell(ETALON_CELLS.SURNAME) != null && !stringValue(it.getCell(ETALON_CELLS.SURNAME)).empty}
        .forEach{ row ->
//    String surname = stringValue(row.getCell(ETALON_CELLS.SURNAME));
//    String name = stringValue(row.getCell(1));
//    String patronym = stringValue(row.getCell(2));
//    String cardid = stringValue(row.getCell(3));
//    String group = stringValue(row.getCell(5));

    def generated = generatedUsers.next()


    row.getCell(ETALON_CELLS.SURNAME).setCellValue(generated.lname as String)
    row.getCell(ETALON_CELLS.NAME).setCellValue(generated.fname  as String)
    row.getCell(ETALON_CELLS.PATRONUM).setCellValue(generated.patronymic  as String)

    row.getCell(ETALON_CELLS.CARDID).setCellValue(generated.carid as Integer)
    //row.getCell(ETALON_CELLS.GROUP).setCellValue(generated.carid  as double)
    row.getCell(ETALON_CELLS.DORMITORY).setCellValue("")
    row.getCell(ETALON_CELLS.EDUCATIONFORM).setCellValue("Б")

    row.getCell(ETALON_CELLS.HOUSE_PHONE, Row.CREATE_NULL_AS_BLANK).setCellValue(generated.homephone as String)
    row.getCell(ETALON_CELLS.MOBILE_PHONE).setCellValue(generated.mobile  as String)
    row.getCell(ETALON_CELLS.GENDER).setCellValue(generated.gender  as String)
    row.getCell(ETALON_CELLS.F_LANGUAGE, Row.CREATE_NULL_AS_BLANK).setCellValue("англ")

    row.getCell(ETALON_CELLS.BIRTH_DATE).setCellValue(asDate(LocalDate.parse(generated.birth as String, dateFormat)))
    row.getCell(ETALON_CELLS.NATIONALITY).setCellValue("Российская Федерация")
    row.getCell(ETALON_CELLS.AREA).setCellValue("Москва")
    row.getCell(ETALON_CELLS.EDU_FORM).setCellValue("О")
    row.getCell(ETALON_CELLS.PRIVILEGIA, Row.CREATE_NULL_AS_BLANK).setCellValue("")

    println("SURNAME = ${row.getCell(ETALON_CELLS.SURNAME)} " +
            "NAME = ${row.getCell(ETALON_CELLS.NAME)} " +
            "PATRONUM = ${row.getCell(ETALON_CELLS.PATRONUM)} " +
            "CARDID = ${row.getCell(ETALON_CELLS.CARDID)} " +
            "GROUP = ${row.getCell(ETALON_CELLS.GROUP)} " +
            "DORMITORY = ${row.getCell(ETALON_CELLS.DORMITORY)} " +
            "EDUCATIONFORM = ${row.getCell(ETALON_CELLS.EDUCATIONFORM)} " +
            "HOUSE_PHONE = ${row.getCell(ETALON_CELLS.HOUSE_PHONE)} " +
            "MOBILE_PHONE = ${row.getCell(ETALON_CELLS.MOBILE_PHONE)} " +
            "GENDER = ${row.getCell(ETALON_CELLS.GENDER)} " +
            "F_LANGUAGE = ${row.getCell(ETALON_CELLS.F_LANGUAGE)} " +
            "BIRTH_DATE = ${row.getCell(ETALON_CELLS.BIRTH_DATE)} " +
            "NATIONALITY = ${row.getCell(ETALON_CELLS.NATIONALITY)} " +
            "AREA = ${row.getCell(ETALON_CELLS.AREA)} " +
            "EDU_FORM = ${row.getCell(ETALON_CELLS.EDU_FORM)} " +
            "PRIVILEGIA = ${row.getCell(ETALON_CELLS.PRIVILEGIA)} " +
            "")

}

def outputStream = new FileOutputStream("newEtalon.xls")
workbook.write(outputStream)
outputStream.close()













