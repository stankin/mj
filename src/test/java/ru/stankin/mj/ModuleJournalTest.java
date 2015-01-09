package ru.stankin.mj;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Test;
import ru.stankin.mj.model.ModuleJournal;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.StreamSupport;

public class ModuleJournalTest {

    @Test
    public void testProcessIncomingDate1() throws Exception {

        Collection<Student> students = getStudents("/information_items_property_2349.xls");

        Assert.assertEquals(335, (long) students.size());

    }

    @Test
    public void testProcessIncomingDate2() throws Exception {
        Collection<Student> students = getStudents("/information_items_property_2446.xls");
        Assert.assertEquals(119, (long) students.size());
    }

    @Test
    public void testProcessIncomingDate3() throws Exception {
        Collection<Student> students = getStudents("/information_items_property_2444.xls");
        Assert.assertEquals(251, (long) students.size());
    }

    @Test
    public void testProcessIncomingDate4() throws Exception {
        Collection<Student> students = getStudents("/information_items_property_2445.xls");
        Assert.assertEquals(196, (long) students.size());
    }

    private Collection<Student> getStudents(String name) throws IOException, InvalidFormatException {
        ModuleJournal mj = new ModuleJournal();

        Storage storage = new Storage();
        mj.setStorage(storage);

        mj.processIncomingDate(ModuleJournalTest.class.getResourceAsStream(name));

        return storage.getStudents();
    }
}