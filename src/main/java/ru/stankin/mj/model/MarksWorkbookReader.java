package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ru.stankin.mj.utils.UtilsKt.stream;

import static java.util.stream.Collectors.joining;

/**
 * Created by nickl on 21.01.17.
 */
public class MarksWorkbookReader {

    public static final String RATING = "Рейтинг";
    public static final String ACCOUMULATED_RATING = "Накопленный Рейтинг";

    private static Set<String> markTypes = Stream.of("М1", "М2", "З", "Э", "К").collect(Collectors.toSet());

    private static final Logger logger = LogManager.getLogger(MarksWorkbookReader.class);

    public static final int YELLOW_MODULE = 16776960;


    public static Stream<Student> modulesFromExcel(InputStream is, String semester) throws IOException, InvalidFormatException {

        Workbook workbook = WorkbookFactory.create(is);

        return IntStream.range(0, workbook.getNumberOfSheets()).boxed().flatMap(i ->
        {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() > 4) {

                DetectedRows detectedRows = detectSubjsRow(sheet);

                detectedRows.modulesrow = sheet.getRow(2);

                Map<Integer, ModulePrototype> modulePrototypesMap;
                try {
                    modulePrototypesMap = buildModulePrototypesMap(detectedRows, semester);
                } catch (Exception e) {
                    throw new RuntimeException("error building modules prototypes on page " + sheet.getSheetName() + " row: " + detectedRows.subjRow.getRowNum() + " :" + e.getMessage(), e);
                }

                logger.debug("modulePrototypesMap=" + modulePrototypesMap);

                return ((Stream<Row>) stream(sheet.iterator())).map(row -> {
                    if (hasValue(row.getCell(0)) && hasValue(row.getCell(1)) && hasValue(row.getCell(2))) {

                        String group = row.getCell(0).getStringCellValue().trim();
                        String surname = row.getCell(1).getStringCellValue().trim();
                        String initials = row.getCell(2).getStringCellValue().trim();
                        String cardid = stringValue(row.getCell(3)).trim();
                        //Student student = storage.getStudentByGroupSurnameInitials(semester, group, surname, initials);
                        Student student = new Student(cardid, group, surname, initials);

                        student.setModules(new ArrayList<>());

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

                            }
                        }

                        return student;
                    } else
                        return null;
                }).filter(Objects::nonNull);

            } else
                return Stream.empty();
        });


    }

    private static class DetectedRows {
        Row factorsrow;
        Row subjRow;
        public Row modulesrow;
    }

    private static DetectedRows detectSubjsRow(Sheet sheet) {

        DetectedRows detectedRows = new DetectedRows();

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
                        detectedRows.factorsrow = prevRow;
                    }
                }
                detectedRows.subjRow = currentRow;
                return detectedRows;
            }
        }
        throw new IllegalArgumentException("no SubjsRow found");
    }

    private static Map<Integer, ModulePrototype> buildModulePrototypesMap(DetectedRows detectedRows, String semester) {
        Map<Integer, ModulePrototype> moduleMap = new LinkedHashMap<>();

        int rainingIndex = -1;
        int accumulatedRainingIndex = -1;


        SubjectColumnInfo subjectInfo = null;

        for (int j = 0; j < detectedRows.modulesrow.getLastCellNum(); j++) {
            Cell cell = detectedRows.modulesrow.getCell(j);
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String header = cell.getStringCellValue().trim();

                if (detectedRows.subjRow.getCell(j) != null) {
                    String subjName = detectedRows.subjRow.getCell(j).getStringCellValue().trim();
                    if (!subjName.isEmpty()) {
                        subjectInfo = new SubjectColumnInfo(subjName, getFactor(detectedRows, j));
                    }
                }

                if (markTypes.contains(header)) {
                    moduleMap.put(j, new ModulePrototype(subjectInfo, header, semester));
                } else if (header.equals("Р")) {
                    rainingIndex = j;
                } else if (header.equals("РН")) {
                    accumulatedRainingIndex = j;
                }

            }
        }


        for (int j = 0; j < detectedRows.subjRow.getLastCellNum(); j++) {
            Cell cell = detectedRows.subjRow.getCell(j);
            if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                String header = cell.getStringCellValue().trim();
                if (header.equals("Р")) {
                    rainingIndex = j;
                } else if (header.equals("РН")) {
                    accumulatedRainingIndex = j;
                }
            }
        }

        moduleMap.put(rainingIndex, new ModulePrototype(rating, "М1", semester)); // M1 чтобы рейтинг показывался в первом столбце
        moduleMap.put(accumulatedRainingIndex, new ModulePrototype(accumulatedRaiting, "М1", semester));

        return moduleMap;
    }

    private static double getFactor(DetectedRows detectedRows, int j) {
        double factor = 0.0;
        if (detectedRows.factorsrow != null) {
            try {
                Cell cell = detectedRows.factorsrow.getCell(j);
                if (cell != null)
                    factor = cell.getNumericCellValue();
            } catch (IllegalStateException ignored) {
            }
        }
        return factor;
    }


    private static int colorToInt(CellStyle style) {

        //logger.debug("fillBackgroundColorColor {}", colorToInt(style.getFillBackgroundColorColor()));
        //logger.debug("FillForegroundColorColor {}", colorToInt(style.getFillForegroundColorColor()));

        int bgInt = colorToInt(style.getFillBackgroundColorColor());
        return (bgInt != -1) ?
                bgInt :
                colorToInt(style.getFillForegroundColorColor());
    }

    private static int colorToInt(Color fillBackgroundColorColor) {
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
            if (aRgb == null)
                return -1;
            return aRgb[1] << 16 | aRgb[2] << 8 | aRgb[3];
        }

        return -1;
    }

    private static class ModulePrototype {
        final SubjectColumnInfo subjColumnInfo;
        final String moduleName;
        private String semester;

        public ModulePrototype(SubjectColumnInfo subjColumnInfo, String moduleName, String semester) {
            this.subjColumnInfo = subjColumnInfo;
            this.moduleName = moduleName;
            this.semester = semester;
        }

        public Module buildModule(String group) {

            Subject subject = new Subject(semester, group, subjColumnInfo.subjName, subjColumnInfo.factor);

            return new Module(subject, moduleName);
        }

        @Override
        public String toString() {
            return "ModulePrototype{" +
                    subjColumnInfo +
                    ", '" + moduleName + '\'' +
                    '}';
        }
    }

    private static final SubjectColumnInfo rating = new SubjectColumnInfo(RATING, 0.0);
    private static final SubjectColumnInfo accumulatedRaiting = new SubjectColumnInfo(ACCOUMULATED_RATING, 0.0);

    private static class SubjectColumnInfo {

        final String subjName;
        final double factor;

        public SubjectColumnInfo(String subjName, double factor) {
            this.subjName = subjName;
            this.factor = factor;
        }

        @Override
        public String toString() {
            return "SubjectColumnInfo{" +
                    "\'" + subjName + '\'' +
                    ", " + factor +
                    '}';
        }
    }


    public static Stream<Student> updateStudentsFromExcel(InputStream inputStream) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        if (!sheet.getSheetName().equals("Общий список"))
            throw new IllegalArgumentException("Загружаемый файл не является Эталоном. Первая страница должна иметь название \"Общий список\"");


        return IntStream.range(1, sheet.getPhysicalNumberOfRows()).mapToObj(i -> {


            Row row = sheet.getRow(i);
            if (row == null)
                return null;


            String surname = stringValue(row.getCell(0));
            String name = stringValue(row.getCell(1));
            String patronym = stringValue(row.getCell(2));
            String cardid = stringValue(row.getCell(3));
            String group = stringValue(row.getCell(5));
            if (surname == null || surname.isEmpty())
                return null;

            Student student = new Student();

            student.surname = surname;
            student.name = name;
            student.patronym = patronym;
            student.cardid = cardid;
            student.stgroup = group;
            student.initials = null;
            //logger.debug("initi student {}", student);
            student.initialsFromNP();
            //logger.debug("Saving student {}", student);

            return student;

        }).filter(Objects::nonNull);

    }


    private static boolean hasValue(Cell cell) {
        return cell != null && !cell.getStringCellValue().isEmpty();
    }

    private static Stream<String> getStringStream(Row row) {
        return stream(row.cellIterator()).map(MarksWorkbookReader::stringValue);
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
                return "";
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue() + "";
            case Cell.CELL_TYPE_ERROR:
                return "Error";
            default:
                return "None";
        }
    }
}
