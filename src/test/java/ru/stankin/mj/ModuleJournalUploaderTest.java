package ru.stankin.mj;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Test;
import ru.stankin.mj.model.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleJournalUploaderTest {

    @Test
    public void testProcessIncomingDate1() throws Exception {

        Collection<Student> students = getStudentMarks("/information_items_property_2349.xls");
        // Collection<Student> studentso = getStudentMarks("/Модули/information_items_property_2349.xls");

//        System.out.println(students.stream().collect(Collectors.groupingBy(s -> s.stgroup,
//                Collectors.counting()
//        )));
//        System.out.println(studentso.stream().collect(Collectors.groupingBy(s -> s.stgroup,
//                Collectors.counting()
//        )));
//
//        System.out.println(students.stream()
//                .filter(s -> s.stgroup.equals("ИДБ-13-04"))
//                .map(s -> s.surname)//.sorted()
//                .collect(Collectors.joining("\n"))
//        );

        Assert.assertEquals(133, (long) students.size());
        Integer collect = students.stream().mapToInt(s -> s.getModules().size()).sum();
        Assert.assertEquals(4067, collect.intValue());

    }


    @Test
    public void testProcessIncomingData4withBlack() throws Exception {
        Collection<Student> students = getStudentMarks("/2 курс II семестр 2014-2015.xls");

        Assert.assertEquals(119, (long) students.size());

        Student pletenev =
                students.stream().filter((Student s) -> s.cardid.equals("2222222")).findAny().get();

        System.out.println("pletenev = " + pletenev);


        Assert.assertEquals(0, pletenev.getModules().stream()
                .filter(m -> m.getSubject().getTitle().equals("Правоведение"))
                .filter(m -> m.getValue() > 0)
                .count());
        Assert.assertEquals(3, pletenev.getModules().stream()
                .filter(m -> m.getSubject().getTitle().equals("Операционные системы"))
                .filter(m -> m.getValue() > 0)
                .count());

        Assert.assertEquals(3751, (long) students.stream().mapToInt(s -> s.getModules().size()).sum());
    }

    private Collection<Student> getStudentMarks(String name) throws IOException, InvalidFormatException {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage(true);
        mj.setStorage(storage);

        mj.updateMarksFromExcel("", ModuleJournalUploaderTest.class.getResourceAsStream(name));

        try (Stream<Student> students = storage.getStudents()) {
            return students.collect(Collectors.toList());
        }
    }

    @Test
    public void testLoadStudentsList() throws Exception {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.updateStudentsFromExcel("2014-1", ModuleJournalUploaderTest.class.getResourceAsStream("/newEtalon.xls"));
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        try (Stream<Student> students = storage.getStudents()) {
            Assert.assertEquals(1753, students.count());
        }


    }
}