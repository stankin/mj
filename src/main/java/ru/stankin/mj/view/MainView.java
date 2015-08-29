package ru.stankin.mj.view;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.easyuploads.MultiFileUpload;
import org.vaadin.easyuploads.UploadField;
import ru.stankin.mj.UserDAO;
import ru.stankin.mj.UserInfo;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;


@CDIView("")
public class MainView extends CustomComponent implements View {

    private static final Logger logger = LogManager.getLogger(MainView.class);

    @Inject
    private UserInfo user;

    @Inject
    private UserDAO userDao;

    @Inject
    private Storage storage;

    @Inject
    private ModuleJournalUploader moduleJournalUploader;

    @Inject
    private ExecutorService ecs;

    MarksTable marks;
    private List<StudentButton> studentButtons;
    private Label studentLabel;

    private AlarmHolder alarmHolder = new AlarmHolder("Загрузка данных", this);
    private ComboBox semestrCbx;

    @Override
    public void enter(ViewChangeEvent event) {

        addStyleName("main");

        logger.debug("entered");

        setHeight("100%");
        VerticalLayout verticalLayout = new VerticalLayout();

//        verticalLayout.setWidth("100%");
//        verticalLayout.setHeight("100%");
        Panel pael1 = new Panel();
        HorizontalLayout content = new HorizontalLayout();
        content.setWidth("100%");
        //content.setHeight(30, Unit.PIXELS);
        Label label = new Label("<b>&nbsp;МОДУЛЬНЫЙ ЖУРНАЛ</b>", ContentMode.HTML);
        label.setWidth(200, Unit.PIXELS);
        content.addComponent(label);
        //content.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
        //content.setExpandRatio(label, 0.5f);
        //label.setHeight(30,Unit.PIXELS);


        content.addComponent(createSemestrCbx());
        content.addComponent(ratingRulesButton());
        if (!user.isAdmin()) {
            StudentRatingButton studentRatingButton = new StudentRatingButton();
            content.addComponent(studentRatingButton);
            Student student = (Student) user.getUser();
            studentRatingButton.setStudent(storage.getStudentById(student.id, getCurrentSemester()));
        }

        Label blonk = new Label("");
        content.addComponent(blonk);
        //content.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
        content.setExpandRatio(blonk, 1);

        Button settings = new Button("Аккаунт: " + user.getName(), event1 -> {
            this.getUI().addWindow(new AccountWindow(user.getUser(), userDao::saveUser, true));
        });
        if (AccountWindow.needChangePassword(user.getUser()))
            settings.click();
        //settings.setEnabled(false);
        content.addComponent(settings);
        content.setComponentAlignment(settings, Alignment.TOP_RIGHT);
        Button exit = new Button("Выход");
        exit.addClickListener(event1 -> {
            user.setUser(null);
            VaadinService.getCurrentRequest().getWrappedSession().invalidate();
            this.getUI().getPage().reload();
        });
        content.addComponent(exit);
        content.setComponentAlignment(exit, Alignment.TOP_RIGHT);

        pael1.setContent(content);

        // pael1.setWidth(100, Unit.PERCENTAGE);
        //  pael1.setHeight(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(pael1);

        Component mainPanel;
        if (user.isAdmin())
            mainPanel = genUploadAndGrids();
        else {
            mainPanel = genMarks();
            Student student = (Student) user.getUser();
            marks.fillMarks(storage.getStudentById(student.id, getCurrentSemester()));
        }

        verticalLayout.addComponent(mainPanel);
        verticalLayout.setExpandRatio(mainPanel, 1);
        verticalLayout.setSizeFull();
        setCompositionRoot(verticalLayout);

    }

    private ComboBox createSemestrCbx() {
        semestrCbx = new ComboBox();
        semestrCbx.setContainerDataSource(new IndexedContainer(Arrays.asList("2014/2015 весна", "2014/2015 осень")));
        semestrCbx.select(semestrCbx.getItemIds().iterator().next());
        semestrCbx.setTextInputAllowed(false);
        semestrCbx.setNullSelectionAllowed(false);
        return semestrCbx;
    }

    private Button ratingRulesButton() {
        return new Button("Правила расчёта рейтинга", event1 -> {
            Window window = new Window("Правила расчёта рейтинга");
            BrowserFrame content1 = new BrowserFrame("Правила расчёта рейтинга", new ExternalResource("rating.html"));
            window.setContent(content1);
            content1.setSizeFull();
            Utils.showCentralWindow(this.getUI(), window);
        });
    }

    private HorizontalLayout genUploadAndGrids() {
        HorizontalLayout uploadAndGrids = new HorizontalLayout();
        //uploadAndGrids.setMargin(true);
        VerticalLayout uploads = new VerticalLayout();
        uploads.setHeight(100, Unit.PERCENTAGE);
        uploads.setMargin(true);
        uploads.setSpacing(true);
        uploads.addComponent(createEtalonUpload());
        uploads.addComponent(createMarksUpload());
        uploads.addComponent(new Button("Удалить все модули", event -> {
            ConfirmDialog.show(this.getUI(), "Удаление всех модулей", ("Вы уверены что хотите удалить все модули?" +
                            " Вам придется перезалить журналы, чтобы модули опять стали доступны"),
                    "Удалить", "Отмена", dialog -> {
                        if (dialog.isConfirmed()) {
                            storage.deleteAllModules();
                            setWorkingStudent(null);
                        }
                    });
        }));
        Label c1 = new Label();
        uploads.addComponent(c1);
        uploads.setExpandRatio(c1, 1);
        Panel panel = new Panel(uploads);
        panel.setWidth(200, Unit.PIXELS);
        panel.setHeight(100, Unit.PERCENTAGE);
        uploadAndGrids.addComponent(panel);

        Component c = buildGrids();
        //TextArea c = new TextArea();
        c.setHeight(100, Unit.PERCENTAGE);
        uploadAndGrids.addComponent(c);
        uploadAndGrids.setExpandRatio(c, 1);
        uploadAndGrids.setSizeFull();
        return uploadAndGrids;
    }

    private Component buildGrids() {

        Table students = new Table();
        students.setWidth(100, Unit.PERCENTAGE);
        students.setHeight(100, Unit.PERCENTAGE);


        students.addContainerProperty("Группа", String.class, null);
        students.setColumnWidth("Группа", 60);
        students.addContainerProperty("Фамилия", String.class, null);
        students.setColumnWidth("Фамилия", 100);
        students.addContainerProperty("ИО", String.class, null);
        students.setColumnWidth("ИО", 30);
        students.addContainerProperty("Логин", String.class, null);
        students.setColumnWidth("Логин", 60);
//        students.addContainerProperty("Пароль", String.class, null);
//        students.setColumnWidth("Пароль", 60);

        students.setEditable(true);
        students.setSelectable(true);
        students.setTableFieldFactory(new TableFieldFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {

                if (propertyId.equals("Пароль")) {
                    Field field = DefaultFieldFactory.get().createField(container, itemId, propertyId, uiContext);
                    field.addValueChangeListener(event -> {
                        logger.debug("Property.ValueChangeEvent:" + event);
                        logger.debug("itemId:" + itemId);
                    });
                    return field;
                } else
                    return null;

            }
        });


        TextField searchField = new TextField();
        searchField.setWidth(100, Unit.PERCENTAGE);
        Label searchLabel = new Label("Поиск");
        searchLabel.setWidth(60, Unit.PIXELS);
        HorizontalLayout searchForm = new HorizontalLayout(searchLabel, searchField);
        searchForm.setExpandRatio(searchField, 1);
        searchForm.setWidth(100, Unit.PERCENTAGE);
        StudentsContainer studentsContainer = new StudentsContainer(storage);
        students.setContainerDataSource(studentsContainer);

        searchField.addTextChangeListener(event1 -> {
            String text = event1.getText();
            //if (text.length() > 2) {
            studentsContainer.setFilter(text);
            //}

        });


        genMarks();

        studentLabel = new Label("", ContentMode.HTML);
        studentLabel.setWidth(200, Unit.PIXELS);
        studentButtons = Arrays.asList(
                new StudentSettingsButton(),
                new StudentRatingButton()
                //new StudentDeleteModulesButton()
        );
        HorizontalLayout studentLine = new HorizontalLayout(studentLabel);

        for (StudentButton stbtn : studentButtons) {
            studentLine.addComponent(stbtn);
        }


        students.addValueChangeListener(event1 -> {
            //logger.debug("selection:{}", event1);
            //logger.debug("stacktacer:{}",new Exception("stacktrace"));
            if (event1.getProperty() == null || event1.getProperty().getValue() == null)
                return;
            setWorkingStudent((Integer) event1.getProperty().getValue());

        });

        GridLayout grid = new GridLayout(2, 3);
        grid.setHeight(100, Unit.PERCENTAGE);
        grid.setWidth(100, Unit.PERCENTAGE);
        //grid.addComponent(upload, 0, 0);
        grid.addComponent(searchForm, 0, 0);
        grid.addComponent(students, 0, 1);
        grid.addComponent(studentLine, 1, 0);
        grid.addComponent(marks, 1, 1);
        grid.setSpacing(true);
        grid.setRowExpandRatio(0, 0);
        grid.setRowExpandRatio(1, 1);
        grid.setColumnExpandRatio(0, 2);
        grid.setColumnExpandRatio(1, 1);

        grid.setMargin(true);
        return /*new Panel(*/grid/*)*/;
    }

    private void setWorkingStudent(Integer studentId) {
        Student student = null;
        if (studentId != null) {
            student = storage.getStudentById(studentId, getCurrentSemester());
            studentLabel.setValue("<b>" + student.surname + " " + student.initials + "</b>");
        } else {
            studentLabel.setValue("");
        }

        marks.fillMarks(student);

        for (StudentButton stbtn : studentButtons) {
            stbtn.setStudent(student);
        }
    }

    private Table genMarks() {
        marks = new MarksTable();
        marks.setSizeFull();
        marks.setWidth(100, Unit.PERCENTAGE);
        return marks;
    }

    private Component createEtalonUpload() {
        final UploadField uploadField2 = new UploadField() {
            @Override
            protected void updateDisplay() {
                alarmHolder.post("Эталон загружен: " + this.getLastFileName());
            }
        };
        uploadField2.setFieldType(UploadField.FieldType.FILE);
        uploadField2.setCaption("Загрузить эталон");
        uploadField2.setButtonCaption("Выбрать файл");
        uploadField2.setFileDeletesAllowed(false);

        uploadField2.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {

                File file = (File) uploadField2.getValue();
                if (file == null)
                    return;

                try {
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                        List<String> log = moduleJournalUploader.updateStudentsFromExcel(bufferedInputStream);
                        alarmHolder.post(String.join("\n", log));
                        uploadField2.setValue(null);
                    }
                } catch (Exception e) {
                    alarmHolder.error(e);
                }

            }
        });

        return uploadField2;
    }


    private MultiFileUpload createMarksUpload() {
        //verticalLayout.setMargin(true);
//        FileReceiver uploadReceiver = new FileReceiver(this, moduleJournalUploader, ecs);
//        Upload upload = new Upload("Загрузка файла", uploadReceiver);
//
//        uploadReceiver.serve(upload);

        MultiFileUpload upload = new MultiFileUpload() {

            @Override
            protected String getAreaText() {
                return "<small>Перетащите<br/>файлы</small>";
            }

            @Override
            protected void handleFile(File file, String fileName,
                                      String mimeType, long length) {
                String msg = "Модульный журнал " + fileName + " загружен";
                try {
                    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
                    List<String> messages = moduleJournalUploader.updateMarksFromExcel(getCurrentSemester(), is);
                    messages.add(0, msg);
                    is.close();
                    String join = String.join("\n", messages);
                    logger.debug("uploadmesages:{}", join);
                    alarmHolder.post(join);
                } catch (Exception e) {
                    logger.error("error processing {}", file, e);
                    alarmHolder.error(e);
                }
            }

//            @Override
//            protected FileBuffer createReceiver() {
//                FileBuffer receiver = super.createReceiver();
//                /*
//                 * Make receiver not to delete files after they have been
//                 * handled by #handleFile().
//                 */
//                receiver.setDeleteFiles(false);
//                return receiver;
//            }
        };
        upload.setCaption("Загрузить файлы с оценками");
        upload.setUploadButtonCaption("Выбрать файлы");
        upload.setRootDirectory(Files.createTempDir().toString());
        return upload;
    }

    public String getCurrentSemester() {
        return (String) semestrCbx.getValue();
    }

    private class StudentButton extends Button {
        protected Student student;

        public StudentButton(String caption) {
            super(caption);
            this.setEnabled(false);
        }

        public Student getStudent() {
            return student;
        }

        public void setStudent(Student student) {
            this.setEnabled(student != null);
            this.student = student;
        }
    }

    private class StudentSettingsButton extends StudentButton {

        public StudentSettingsButton() {
            super("Редактировать");
            this.addClickListener(event -> this.getUI().addWindow(new AccountWindow(student, userDao::saveUser)));
        }

    }

    private class StudentRatingButton extends StudentButton {

        public StudentRatingButton() {
            super("Расcчитать рейтинг");
            this.addClickListener(event -> {
                this.setStudent(storage.getStudentById(this.student.id, getCurrentSemester()));
                RatingCalculationTable ratingCalculationTable = new RatingCalculationTable(student);
                ratingCalculationTable.setSizeFull();
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.addComponent(ratingCalculationTable);
                verticalLayout.setExpandRatio(ratingCalculationTable, 1);
                verticalLayout.addComponent(new Label("Если вы хотите спрогнозировать свой рейтинг," +
                        " вы можете ввести недостающие модули в этой форме"));
                verticalLayout.setSizeFull();
                Window window = new Window("Расчет рейтинга", verticalLayout);

                Utils.showCentralWindow(this.getUI(), window);
                Page.getCurrent().getJavaScript().execute("yaCounter29801259.hit('#calc');");
            } );
        }

    }

    private class StudentDeleteModulesButton extends StudentButton {

        private List<Module> oldModules = null;

        public StudentDeleteModulesButton() {
            super("Удалить Модули");
            this.addClickListener(event -> {
                int studentId = student.id;
                if (oldModules == null) {
                    storage.deleteStudentModules(student);
                    oldModules = student.getModules();
                    oldModules.forEach(m -> m.setId(0));
                    setCaption("Восстановить Модули");
                } else {
                    storage.updateModules(student);
                    student = null;
                }
                setWorkingStudent(studentId);
            });
        }

        @Override
        public void setStudent(Student student) {
            if (student == null || this.student == null || this.student.id != student.id) {
                setCaption("Удалить Модули");
                oldModules = null;
                super.setStudent(student);
            }
        }
    }


}

