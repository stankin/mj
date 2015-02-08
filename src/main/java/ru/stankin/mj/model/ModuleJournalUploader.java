package ru.stankin.mj.model;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

//@ApplicationScoped
@Singleton
public class ModuleJournalUploader {

    private static final Logger logger = LogManager.getLogger(ModuleJournalUploader.class);


    @Inject
    private Storage storage;

    private static Set<String> markTypes = Arrays.asList("З", "Э").stream().collect(Collectors.toSet());

    public void processIncomingDate(InputStream is) throws IOException, InvalidFormatException {

        Workbook workbook = WorkbookFactory.create(is);

        new WorkbookReader(workbook).writeTo(storage);


        is.close();

//        ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream
//                ("students.bin")));
//
//        outputStream.writeObject(studentList);
//
//        outputStream.close();


    }

    static class WorkbookReader {

        private Workbook workbook;

        public WorkbookReader(Workbook workbook) {
            this.workbook = workbook;
        }

        void writeTo(Storage storage) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 4) {

                    Row subjRow = detectSubjsRow(sheet);

                    Row modulesrow = sheet.getRow(2);

                    Map<Integer, Module> moduleMap = new LinkedHashMap<>();

                    for (int j = 0; j < modulesrow.getLastCellNum(); j++) {

                        Cell cell = modulesrow.getCell(j);
                        if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                            if (cell.getStringCellValue().trim().equals("М1")) {
                                String subject = subjRow.getCell(j).getStringCellValue().trim();
                                logger.debug(j + " " + subject);
                                if (!modulesrow.getCell(j + 1).getStringCellValue().trim().equals("М2"))
                                    throw new IllegalArgumentException("no m2");

                                moduleMap.put(j, new Module(subject, "М1"));
                                moduleMap.put(j + 1, new Module(subject, "М2"));

                                for (int k = 2; k < 5; k++) {
                                    String markType = modulesrow.getCell(j + k).getStringCellValue().trim();
                                    if (markType.equals("М1"))
                                        break;

                                    if (markTypes.contains(markType))
                                        moduleMap.put(j + k, new Module(subject, markType));
                                }

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

                            for (Map.Entry<Integer, Module> entry : moduleMap.entrySet()) {
                                Module module = entry.getValue().clone();
                                Cell cell = row.getCell(entry.getKey());
                                if (cell != null) {
                                    module.color = colorToInt(cell.getCellStyle());
                                    module.value = (int) cell.getNumericCellValue();
                                }
                                else
                                    module.value = 0;
                                student.getModules().add(module);
                            }

                            storage.updateModules(student);

                            logger.debug("Student: {}", student);

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

        }

        private int colorToInt(CellStyle style) {

            //logger.debug("fillBackgroundColorColor {}", style.getFillBackgroundColorColor());
            //logger.debug("FillForegroundColorColor {}", style.getFillForegroundColorColor());

            int bgInt = colorToInt(style.getFillBackgroundColorColor());
            return  (bgInt !=0) ?
                    bgInt:
                    colorToInt(style.getFillForegroundColorColor());
        }

        private int colorToInt(Color fillBackgroundColorColor) {
            if(fillBackgroundColorColor instanceof HSSFColor){
                HSSFColor hssfColor = (HSSFColor) fillBackgroundColorColor;
                //logger.debug("processing color {}", hssfColor);
                //logger.debug("processing colorhex {}", hssfColor.getHexString());
                short[] triplet = hssfColor.getTriplet();
                //logger.debug("HSSFColor {}", Arrays.toString(triplet));
                return triplet[0] << 16 | triplet[1] << 8 | triplet[2];
            }
            if(fillBackgroundColorColor instanceof XSSFColor){
                byte[] aRgb = ((XSSFColor) fillBackgroundColorColor).getARgb();
                //logger.debug("XSSFColor {}", Arrays.toString(aRgb));
                return aRgb[1] << 16 | aRgb[2] << 8 | aRgb[3];
            }

            return 0;
        }

        private Row detectSubjsRow(Sheet sheet) {

            for (int i = 0; i < sheet.getLastRowNum(); i++) {


                Row subjRow = sheet.getRow(i);
                if (subjRow == null)
                    continue;
                //System.out.println("tabrow:" + rowTabsepareted(subjRow));
                List<String> subjsList = getStringStream(subjRow).filter(str -> !str.matches("\\s*")).collect(Collectors
                        .toList());

                logger.debug("subjsList: {} {}", subjsList, subjsList.size());
                if (!subjsList.isEmpty())
                    return subjRow;
            }
            throw new IllegalArgumentException("no SubjsRow found");
        }


        private String rowTabsepareted(Row row) {
            return getStringStream(row).collect(Collectors.joining("\t"));
        }

        private Stream<String> getStringStream(Row row) {
            return stream(row.cellIterator()).map(this::strigValue);
        }

        private String strigValue(Cell cell) {
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


        public <T> Stream<T> stream(Iterator<T> sourceIterator) {
            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(sourceIterator, Spliterator.ORDERED),
                    false);
        }

    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
