package ru.stankin.mj;

import org.junit.Test;
import ru.stankin.mj.model.ModuleJournal;

public class ModuleJournalTest {

    @Test
    public void testProcessIncomingDate() throws Exception {

        ModuleJournal mj = new ModuleJournal();

        mj.processIncomingDate(ModuleJournalTest.class.getResourceAsStream("/information_items_property_2349.xls"));



    }
}