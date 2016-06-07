package ru.stankin.mj.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

//@ApplicationScoped
@Singleton
public class ModuleJournalUploader {

    public static final String RATING = "Рейтинг";
    public static final String ACCOUMULATED_RATING = "Накопленный Рейтинг";
    private static final Logger logger = LogManager.getLogger(ModuleJournalUploader.class);


    @Inject
    private Storage storage;

    private static Set<String> markTypes = Arrays.asList("З", "Э", "К").stream().collect(Collectors.toSet());

    public List<String> updateMarksFromExcel(String semester, InputStream is) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(is);
        List<String> strings = new MarksWorkbookReader(semester, workbook, storage).writeToStorage();
        is.close();

//        ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream
//                ("students.bin")));
//
//        outputStream.writeObject(studentList);
//
//        outputStream.close();

        return strings;
    }

    @javax.transaction.Transactional
    public List<String> updateStudentsFromExcel(String semestr, InputStream inputStream) throws IOException, InvalidFormatException {

        Workbook workbook = WorkbookFactory.create(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        if (!sheet.getSheetName().equals("Общий список"))
            throw new IllegalArgumentException("Загружаемый файл не является Эталоном. Первая страница должна иметь название \"Общий список\"");

        Set<String> processedCards = new HashSet<>();

        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {

            Row row = sheet.getRow(i);
            if (row == null)
                continue;


            String surname = stringValue(row.getCell(0));
            String name = stringValue(row.getCell(1));
            String patronym = stringValue(row.getCell(2));
            String cardid = stringValue(row.getCell(3));
            String group = stringValue(row.getCell(5));
            if (surname == null || surname.isEmpty())
                continue;

            Student student = null;
            student = storage.getStudentByCardId(cardid);
            processedCards.add(cardid);
            if (student == null)
                student = new Student();

            student.surname = surname;
            student.name = name;
            student.patronym = patronym;
            student.cardid = cardid;
            student.stgroup = group;
            student.initials = null;
            //logger.debug("initi student {}", student);
            student.initialsFromNP();
            //logger.debug("Saving student {}", student);
            storage.saveStudent(student, semestr);

        }

        List<String> messages = new ArrayList<>();

        storage.getStudents().filter(s -> !processedCards.contains(s.cardid)).forEach(s -> {
            storage.deleteStudent(s);
            messages.add("Удяляем студента:" + s.cardid + " " + s.getGroups().stream().map(g -> g.groupName).collect(joining(", ")) + " " + s.surname + " " + s.initials);
        });


        inputStream.close();
        return messages;
    }

    static class MarksWorkbookReader {

        public static final int YELLOW_MODULE = 16776960;
        private Workbook workbook;

        Storage storage;

        private Row subjRow;
        private Row modulesrow;
        private Row factorsrow;

        private String semester;

        public MarksWorkbookReader(String semester, Workbook workbook, Storage storage) {
            this.semester = semester;
            this.workbook = workbook;
            this.storage = storage;
        }

        List<String> writeToStorage() {

            List<String> messages = new ArrayList<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 4) {

                    detectSubjsRow(sheet);

                    modulesrow = sheet.getRow(2);


                    Map<Integer, ModulePrototype> modulePrototypesMap = buildModulePrototypesMap();

                    logger.debug("modulePrototypesMap=" + modulePrototypesMap);

                    sheet.iterator().forEachRemaining(row -> {
                        if (hasValue(row.getCell(0)) && hasValue(row.getCell(1)) && hasValue(row.getCell(2))) {

                            String group = row.getCell(0).getStringCellValue().trim();
                            String surname = row.getCell(1).getStringCellValue().trim();
                            String initials = row.getCell(2).getStringCellValue().trim();
                            String cardid = stringValue(row.getCell(3)).trim();
                            //Student student = storage.getStudentByGroupSurnameInitials(semester, group, surname, initials);
                            Student student = storage.getStudentByCardId(cardid);


                            if (student == null) {
                                logger.debug("Не найден студент " + group + " " + surname + " " + initials + " в " + semester);
                                messages.add("Не найден студент " + group + " " + surname + " " + initials + " в " + semester);
                            } else {

                                student.setModules(new ArrayList<>());
                             /*new Student(
                                    row.getCell(0).getStringCellValue(),
                                    row.getCell(1).getStringCellValue(),
                                    row.getCell(2).getStringCellValue()
                            ); */

                                for (Map.Entry<Integer, ModulePrototype> entry : modulePrototypesMap.entrySet()) {
                                    Module module = entry.getValue().buildModule(group);
                                    //logger.debug("module=" + module);
                                    Cell cell = row.getCell(entry.getKey());
                                    if (cell != null) {
                                        module.setColor(colorToInt(cell.getCellStyle()));
                                        try {
                                            module.setValue((int) Math.round(cell.getNumericCellValue()));
                                        } catch (Exception e) {
                                            //logger.debug("error reading numeric data: " + e.getMessage());
                                            module.setValue(0);
                                        }
                                    } else
                                        module.setValue(0);
                                    if (module.getColor() != 0) {
                                        student.getModules().add(module);
                                        if (module.getColor() == YELLOW_MODULE && module.getValue() > 25) {
                                            messages.add("Просроченный модуль > 25 :"
                                                    + student.stgroup + " "
                                                    + student.surname + " "
                                                    + student.initials + " "
                                                    + module.getSubject().getTitle() + ": "
                                                    + module.getValue()
                                            );
                                        }
                                    }
                                }

                                storage.updateModules(student);

                                //logger.debug("Student: {}", student);
                            }

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

            return messages;

        }

        private Row detectSubjsRow(Sheet sheet) {
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow == null)
                    continue;
                List<String> subjsList = getStringStream(currentRow)
                        .filter(str -> !str.matches("(\\s*|\\d+([\\.,]\\d*)?)"))
                        .collect(Collectors.toList());
                logger.debug("subjsList: {} {}", subjsList, subjsList.size());
                if (!subjsList.isEmpty()) {
                    Row prevRow = sheet.getRow(i - 1);
                    if (prevRow != null) {
                        List<String> factorsList = getStringStream(prevRow)
                                .filter(str -> str.matches("\\d+([\\.,]\\d*)?"))
                                .collect(Collectors.toList());
                        logger.debug("factorsList = {}", factorsList);
                        if (!factorsList.isEmpty()) {
                            this.factorsrow = prevRow;
                        }
                    }
                    return this.subjRow = currentRow;
                }
            }
            throw new IllegalArgumentException("no SubjsRow found");
        }

        private Map<Integer, ModulePrototype> buildModulePrototypesMap() {
            Map<Integer, ModulePrototype> moduleMap = new LinkedHashMap<>();

            int rainingIndex = -1;
            int accumulatedRainingIndex = -1;


            for (int j = 0; j < modulesrow.getLastCellNum(); j++) {
                Cell cell = modulesrow.getCell(j);
                if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    String header = cell.getStringCellValue().trim();
                    if (header.equals("М1")) {
                        String subjName = subjRow.getCell(j).getStringCellValue().trim();

                        SubjectColumnInfo subjectInfo = new SubjectColumnInfo(subjName, getFactor(j));

                        if (!modulesrow.getCell(j + 1).getStringCellValue().trim().equals("М2"))
                            throw new IllegalArgumentException("no m2");

                        moduleMap.put(j, new ModulePrototype(subjectInfo, "М1"));
                        moduleMap.put(j + 1, new ModulePrototype(subjectInfo, "М2"));


                        for (int k = 2; k < 5; k++) {
                            String markType = modulesrow.getCell(j + k).getStringCellValue().trim();
                            //logger.debug("markType {}", markType);
                            if (markType.equals("М1"))
                                break;

                            boolean contains = markTypes.contains(markType);
                            //logger.debug("contains {}", contains);
                            if (contains)
                                moduleMap.put(j + k, new ModulePrototype(subjectInfo, markType));
                        }

                    } else if (header.equals("Р")) {
                        rainingIndex = j;
                    } else if (header.equals("РН")) {
                        accumulatedRainingIndex = j;
                    }

                }
            }


            for (int j = 0; j < subjRow.getLastCellNum(); j++) {
                Cell cell = subjRow.getCell(j);
                if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    String header = cell.getStringCellValue().trim();
                    if (header.equals("Р")) {
                        rainingIndex = j;
                    } else if (header.equals("РН")) {
                        accumulatedRainingIndex = j;
                    }
                }
            }

            moduleMap.put(rainingIndex, new ModulePrototype(rating, "М1"));
            moduleMap.put(accumulatedRainingIndex, new ModulePrototype(accumulatedRaiting, "М1"));

            return moduleMap;
        }

        private double getFactor(int j) {
            double factor = 0.0;
            if (factorsrow != null) {
                try {
                    Cell cell = factorsrow.getCell(j);
                    if (cell != null)
                        factor = cell.getNumericCellValue();
                } catch (IllegalStateException ignored) {
                }
            }
            return factor;
        }


        private int colorToInt(CellStyle style) {

            //logger.debug("fillBackgroundColorColor {}", colorToInt(style.getFillBackgroundColorColor()));
            //logger.debug("FillForegroundColorColor {}", colorToInt(style.getFillForegroundColorColor()));

            int bgInt = colorToInt(style.getFillBackgroundColorColor());
            return (bgInt != -1) ?
                    bgInt :
                    colorToInt(style.getFillForegroundColorColor());
        }

        private int colorToInt(Color fillBackgroundColorColor) {
            if (fillBackgroundColorColor instanceof HSSFColor) {
                HSSFColor hssfColor = (HSSFColor) fillBackgroundColorColor;
                //logger.debug("processing color {}", hssfColor);
                //logger.debug("processing color index {}", hssfColor.getIndex());
                if (hssfColor.getIndex() == HSSFColor.AUTOMATIC.index)
                    return -1;
                //logger.debug("processing colorhex {}", hssfColor.getHexString());
                short[] triplet = hssfColor.getTriplet();
                //logger.debug("HSSFColor {}", Arrays.toString(triplet));
                return triplet[0] << 16 | triplet[1] << 8 | triplet[2];
            }
            if (fillBackgroundColorColor instanceof XSSFColor) {
                XSSFColor xssfColor = (XSSFColor) fillBackgroundColorColor;
                if (xssfColor.isAuto())
                    return -1;
                byte[] aRgb = xssfColor.getARgb();
                logger.debug("XSSFColor {}", Arrays.toString(aRgb));
                return aRgb[1] << 16 | aRgb[2] << 8 | aRgb[3];
            }

            return -1;
        }

        private class ModulePrototype {
            final SubjectColumnInfo subjColumnInfo;
            final String moduleName;

            public ModulePrototype(SubjectColumnInfo subjColumnInfo, String moduleName) {
                this.subjColumnInfo = subjColumnInfo;
                this.moduleName = moduleName;
            }

            public Module buildModule(String group) {
                return new Module(storage.getOrCreateSubject(semester, group, subjColumnInfo.subjName, subjColumnInfo.factor), moduleName);
            }
        }

        private final SubjectColumnInfo rating = new SubjectColumnInfo(RATING, 0.0);
        private final SubjectColumnInfo accumulatedRaiting = new SubjectColumnInfo(ACCOUMULATED_RATING, 0.0);

        private class SubjectColumnInfo {

            final String subjName;
            final double factor;

            public SubjectColumnInfo(String subjName, double factor) {
                this.subjName = subjName;
                this.factor = factor;
            }
        }
    }

    private static boolean hasValue(Cell cell) {
        return cell != null && !cell.getStringCellValue().isEmpty();
    }

    private static Stream<String> getStringStream(Row row) {
        return stream(row.cellIterator()).map(ModuleJournalUploader::stringValue);
    }


    private static DataFormatter dataFormatter = new DataFormatter();

    public static String stringValue(Cell cell) {
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_NUMERIC:
                return dataFormatter.formatCellValue(cell);
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue().trim();
            case Cell.CELL_TYPE_FORMULA:
                return "FORMULA";
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue() + "";
            case Cell.CELL_TYPE_ERROR:
                return "Error";
            default:
                return "None";
        }
    }

    public static <T> Stream<T> stream(Iterator<T> sourceIterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sourceIterator, Spliterator.ORDERED),
                false);
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
