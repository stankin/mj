package ru.stankin.mj.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.sql2o.Sql2o;
import ru.stankin.mj.utils.ThreadLocalTransaction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static ru.stankin.mj.model.Module.YELLOW_MODULE;

//@ApplicationScoped
@Singleton
public class ModuleJournalUploader {
    private static final Logger logger = LogManager.getLogger(ModuleJournalUploader.class);
    @Inject
    private StudentsStorage storage;
    @Inject
    private ModulesStorage modules;
    @Inject
    private Sql2o sql2o;

    public List<String> updateMarksFromExcel(String semester, InputStream is) throws IOException, InvalidFormatException {
        try {
            List<String> messages = new ArrayList<>();
            ModulesUpdateStat modulesUpdateStat = new ModulesUpdateStat(0, 0, 0);
            ThreadLocalTransaction.INSTANCE.within(sql2o, () -> {
                        try {
                            Stream<Student> modulesFromExcel = MarksWorkbookReader.modulesFromExcel(is, semester);
                            modulesFromExcel.forEach(studentFromExcel -> {
                                Student student = storage.getStudentByCardId(studentFromExcel.cardid);
                                if (student == null) {
                                    final String m = "Не найден студент " + studentFromExcel.stgroup + " " + studentFromExcel.surname + " " + studentFromExcel.initials + " " + studentFromExcel.cardid + " в " + semester;
                                    logger.debug(m);
                                    messages.add(m);
                                    return;
                                }
                                student.setModules(studentFromExcel.getModules());
                                for (Module module : student.getModules()) {
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
                                modulesUpdateStat.plusAssign(modules.updateModules(student));
                            });
                            messages.add(0, "Модулей: добавлено: " + modulesUpdateStat.added +
                                    ", обновлено: " + modulesUpdateStat.updated +
                                    ", удалено: " + modulesUpdateStat.deleted);
                        } catch (IOException | InvalidFormatException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
            );
            return messages;
        } finally {
            is.close();
        }
    }

    public List<String> updateStudentsFromExcel(String semestr, InputStream inputStream) throws
            IOException, InvalidFormatException {
        try {
            return ThreadLocalTransaction.INSTANCE.within(sql2o, () -> {
                try {
                    Set<String> processedCards = new HashSet<>();
                    MarksWorkbookReader.updateStudentsFromExcel(inputStream).forEach(excelStudent -> {
                        Student student = storage.getStudentByCardId(excelStudent.cardid);
                        if (student == null)
                            student = excelStudent;
                        else {
                            student.surname = excelStudent.surname;
                            student.name = excelStudent.name;
                            student.patronym = excelStudent.patronym;
                            student.cardid = excelStudent.cardid;
                            student.stgroup = excelStudent.stgroup;
                            student.initials = null;
                            //logger.debug("initi student {}", student);
                            student.initialsFromNP();
                        }
                        processedCards.add(student.cardid);
                        storage.saveStudent(student, semestr);
                    });
                    List<String> messages = new ArrayList<>();
                    try (Stream<Student> students = storage.getStudents()) {
                        students.filter(s -> !processedCards.contains(s.cardid)).forEach(s -> {
                            storage.deleteStudent(s);
                            messages.add("Удяляем студента:" + s.cardid + " " + s.getGroups().stream().map(g -> g.groupName).collect(joining(", ")) + " " + s.surname + " " + s.initials);
                        });
                    }
                    return messages;
                } catch (IOException | InvalidFormatException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            inputStream.close();
        }
    }
}
