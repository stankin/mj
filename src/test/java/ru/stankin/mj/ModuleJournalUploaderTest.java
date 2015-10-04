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

        Assert.assertEquals(335, (long) students.size());
        Integer collect = students.stream().map(s -> s.getModules().size()).collect(Collectors.summingInt(i -> i));
        Assert.assertEquals(10922, collect.intValue());

    }

    @Test
    public void testProcessIncomingDate2() throws Exception {
        Collection<Student> students = getStudentMarks("/information_items_property_2446.xls");
        Assert.assertEquals(119, (long) students.size());
    }

    @Test
    public void testProcessIncomingDate3() throws Exception {
        Collection<Student> students = getStudentMarks("/information_items_property_2444.xls");
        Assert.assertEquals(251, (long) students.size());
    }

    @Test
    public void testProcessIncomingDate4withBlack() throws Exception {
        Collection<Student> students = getStudentMarks("/information_items_property_2445.xls");

        Assert.assertEquals(196, (long) students.size());

        Student pletenev =
                students.stream().filter((Student s) -> s.surname.equals("Воронин")).findFirst().get();
        System.out.println("pletenev=" + pletenev);


        Assert.assertEquals(0, pletenev.getModules().stream()
                .filter(m -> m.getSubject().equals("Управление роботами и робототехнич. системами"))
                .count());

        Assert.assertEquals(5980, (long) students.stream().mapToInt(s -> s.getModules().size()).sum());
    }

    @Test
    public void testProcessIncomingWithRatings() throws Exception {
        Collection<Student> students = getStudentMarks("/4 курс II семестр 2014-2015.xls");
        System.out.println(students);
        Assert.assertEquals(194, (long) students.size());

        List<Subject> subjectList = students.stream().flatMap(s -> s.getModules().stream())
                .map(m -> m.getSubject()).distinct().collect(Collectors.toList());

        subjectList.stream().forEach(System.out::println);


    }

    private Collection<Student> getStudentMarks(String name) throws IOException, InvalidFormatException {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.updateMarksFromExcel("", ModuleJournalUploaderTest.class.getResourceAsStream(name));

        Stream<Student> students = storage.getStudents();
        return students.collect(Collectors.toList());
    }

    @Test
    public void testLoadStudentsList() throws Exception {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/newEtalon.xls"));
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        Assert.assertEquals(1753, storage.getStudents().count());


    }
}