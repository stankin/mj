package ru.stankin.mj;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Test;
import ru.stankin.mj.model.MemoryStorage;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import java.io.IOException;
import java.util.Collection;

public class ModuleJournalUploaderTest {

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
        ModuleJournalUploader mj = new ModuleJournalUploader();

        Storage storage = new MemoryStorage();
        mj.setStorage(storage);

        mj.processIncomingDate(ModuleJournalUploaderTest.class.getResourceAsStream(name));

        return storage.getStudents();
    }
}