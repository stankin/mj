package ru.stankin.mj.model;


import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ModuleJournal {


    @Inject
    private Storage storage;


    public void processIncomingDate(InputStream is) throws IOException, InvalidFormatException {

        Workbook workbook = WorkbookFactory.create(is);


        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() > 4) {
                int lastRowNum = sheet.getLastRowNum();
                Row subjRow = sheet.getRow(0);
                //System.out.println("tabrow:" + rowTabsepareted(subjRow));
                List<String> subjsList = getStringStream(subjRow).filter(str -> !str.isEmpty()).collect(Collectors
                        .toList());

                System.out.println("subjsList:" + subjsList);

                Row modulesrow = sheet.getRow(2);

                Map<Integer, Module> moduleMap = new LinkedHashMap<>();

                for (int j = 0; j < modulesrow.getLastCellNum(); j++) {

                    Cell cell = modulesrow.getCell(j);
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                        if (cell.getStringCellValue().equals("М1")) {
                            String subject = subjRow.getCell(j).getStringCellValue();
                            System.out.println(j + " " + subject);
                            if (!modulesrow.getCell(j + 1).getStringCellValue().equals("М2"))
                                throw new IllegalArgumentException("no m2");

                            moduleMap.put(j, new Module(subject, 1));
                            moduleMap.put(j + 1, new Module(subject, 2));
                        }
                    }

                }

                sheet.iterator().forEachRemaining(row -> {
                    if (row.getCell(0) != null && !row.getCell(0).getStringCellValue().isEmpty()) {

                        Student student = new Student(
                                row.getCell(0).getStringCellValue(),
                                row.getCell(1).getStringCellValue(),
                                row.getCell(2).getStringCellValue()
                        );

                        storage.updateModules(student);

                        for (Map.Entry<Integer, Module> entry : moduleMap.entrySet()) {
                            Module module = entry.getValue().clone();
                            Cell cell = row.getCell(entry.getKey());
                            if (cell != null)
                                module.value = (int) cell.getNumericCellValue();
                            else
                                module.value = 0;
                            student.modules.add(module);
                        }

                        System.out.println("Student:" + student);

                    }
                });


//                for (int j = 0; j <= lastRowNum; j++) {
//                    Row row = sheet.getRow(j);
//
//                    String tabrow = rowTabsepareted(row);
//
//                    System.out.println("tabrow:"+tabrow);
//
//                }
            }
        }

        is.close();

//        ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream
//                ("students.bin")));
//
//        outputStream.writeObject(studentList);
//
//        outputStream.close();


    }

    private String rowTabsepareted(Row row) {
        return getStringStream(row).collect(Collectors.joining("\t"));
    }

    private Stream<String> getStringStream(Row row) {
        return stream(row.cellIterator()).map(ModuleJournal::strigValue);
    }

    private static String strigValue(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK: return "";
            case Cell.CELL_TYPE_NUMERIC: return cell.getNumericCellValue() + "";
            case Cell.CELL_TYPE_STRING: return cell.getStringCellValue();
            case Cell.CELL_TYPE_FORMULA: return "FORMULA";
            case Cell.CELL_TYPE_BOOLEAN: return cell.getBooleanCellValue() + "";
            case Cell.CELL_TYPE_ERROR: return "Error";
            default: return "None";
        }
    }


    public static <T> Stream<T> stream(Iterator<T> sourceIterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sourceIterator, Spliterator.ORDERED),
                false);
    }

}
