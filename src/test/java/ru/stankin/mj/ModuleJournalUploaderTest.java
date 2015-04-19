package ru.stankin.mj;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.stankin.mj.model.MemoryStorage;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModuleJournalUploaderTest {

    @Test
    public void testProcessIncomingDate1() throws Exception {

        Collection<Student> students = getStudentMarks("/information_items_property_2349.xls");

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
    public void testProcessIncomingDate4() throws Exception {
        Collection<Student> students = getStudentMarks("/information_items_property_2445.xls");

        Assert.assertEquals(196, (long) students.size());

        Student pletenev =
                students.stream().filter((Student s) -> s.surname.equals("Плетенев")).findFirst().get();
        System.out.println("pletenev=" + pletenev);


        Assert.assertEquals(0, pletenev.getModules().stream()
                .filter(m -> m.getSubject().equals("Управление роботами и робототехнич. системами"))
                .count());

        Assert.assertEquals(5784, (long) students.stream().mapToInt(s -> s.getModules().size()).sum());
    }

    private Collection<Student> getStudentMarks(String name) throws IOException, InvalidFormatException {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.updateMarksFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream(name));

        return storage.getStudents().collect(Collectors.toList());
    }

    @Test
    @Ignore
    public void testLoadStudentsList() throws Exception {
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));


    }
}