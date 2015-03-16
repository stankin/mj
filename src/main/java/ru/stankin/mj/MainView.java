package ru.stankin.mj;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.easyuploads.MultiFileUpload;
import org.vaadin.easyuploads.UploadField;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
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

    Table marks;

    @Override
    public void enter(ViewChangeEvent event) {

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
        content.addComponent(label);
        //content.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
        content.setExpandRatio(label, 1);
        //label.setHeight(30,Unit.PIXELS);
        Button settings = new Button("Аккаунт: " + user.getName(), event1 -> {
            this.getUI().addWindow(new AccountWindow(user.getUser(), userDao::saveUser));
        });
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
            fillMarks(storage.getStudentById(student.id, true));
        }

        verticalLayout.addComponent(mainPanel);
        verticalLayout.setExpandRatio(mainPanel, 1);
        Label bbref = new Label("<div align=\"right\" style=\"margin-right: 20px;\">ФГБОУ ВПО МГТУ СТАНКИН, факультет «Информационных технологий и систем управления»&nbsp;&nbsp;<a href=\"https://bitbucket.org/NicolayMitropolsky/stankin-mj\">Разработка</a></div>", ContentMode.HTML);
        bbref.setWidth(100, Unit.PERCENTAGE);
        //Button bbref = new Button("hhhh");
        //Label spacer = new Label("66");
        //spacer.setWidth(100, Unit.PERCENTAGE);;
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        //horizontalLayout.addComponent(spacer);
        //horizontalLayout.setExpandRatio(spacer, 1);
        horizontalLayout.addComponent(bbref);
        //horizontalLayout.setExpandRatio(bbref, 0);
        horizontalLayout.setComponentAlignment(bbref, Alignment.BOTTOM_RIGHT);
        horizontalLayout.setWidth(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(horizontalLayout);
        verticalLayout.setSizeFull();
        setCompositionRoot(verticalLayout);

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

        Label label = new Label("", ContentMode.HTML);
        label.setWidth(200, Unit.PIXELS);
        StudentSettingsButton studenSettings = new StudentSettingsButton();
        HorizontalLayout studentLine = new HorizontalLayout(label, studenSettings);
        students.addValueChangeListener(event1 -> {
            logger.debug("selection:{}", event1);
            //logger.debug("stacktacer:{}",new Exception("stacktrace"));
            if (event1.getProperty() == null || event1.getProperty().getValue() == null)
                return;
            Student student = storage.getStudentById((Integer) event1.getProperty().getValue(), true);
            label.setValue("<b>" + student.surname + " " + student.initials + "</b>");

            fillMarks(student);

            studenSettings.setStudent(student);

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

    private Table genMarks() {
        marks = new Table();

        marks.addContainerProperty("Предмет", String.class, null);
        marks.setColumnWidth("Предмет", 200);
        marks.addContainerProperty("М1", AbstractComponent.class, null);
        marks.setColumnWidth("М1", 30);
        marks.addContainerProperty("М2", AbstractComponent.class, null);
        marks.setColumnWidth("М2", 30);
        marks.addContainerProperty("К", AbstractComponent.class, null);
        marks.setColumnWidth("К", 30);
        marks.addContainerProperty("З", AbstractComponent.class, null);
        marks.setColumnWidth("З", 30);
        marks.addContainerProperty("Э", AbstractComponent.class, null);
        marks.setColumnWidth("Э", 30);

        marks.setSizeFull();
        marks.setWidth(100, Unit.PERCENTAGE);
        return marks;
    }

    private void fillMarks(Student student) {
        marks.removeAllItems();

        int size = student.getModules().size();
        logger.debug("student has:{} modules", size);
        int i = 0;
        Map<String, Map<String, Module>> modulesGrouped = student.getModulesGrouped();
        logger.debug("modulesGrouped:{} ", modulesGrouped);

        for (Map.Entry<String, Map<String, Module>> subj : modulesGrouped.entrySet()) {


            String subject = subj.getKey();
            Module m1 = subj.getValue().get("М1");
            Module m2 = subj.getValue().get("М2");
            Module m3 = subj.getValue().get("К");
            Module m4 = subj.getValue().get("З");
            Module m5 = subj.getValue().get("Э");
            marks.addItem(
                    new Object[]{subject, inNotNull(m1), inNotNull(m2), inNotNull(m3), inNotNull(m4), inNotNull(m5)},
                    i++);
        }
    }

    private Component createEtalonUpload() {
        final UploadField uploadField2 = new UploadField();
        uploadField2.setFieldType(UploadField.FieldType.FILE);
        uploadField2.setCaption("Загрузить эталон");
        uploadField2.setButtonCaption("Выбрать файл");
        uploadField2.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {

                File file = (File) uploadField2.getValue();
                if (file == null)
                    return;

                try {
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                        moduleJournalUploader.updateStudentsFromExcel(bufferedInputStream);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                String msg = fileName + " uploaded. Saved to file "
                        + file.getAbsolutePath() + " (size " + length
                        + " bytes)";
                try {
                    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
                    List<String> messages = moduleJournalUploader.updateMarksFromExcel(is);
                    messages.add(0, msg);
                    is.close();
                    String join = String.join("\n", messages);
                    logger.debug("uploadmesages:{}", join);
                    Notification.show(join);
                } catch (Exception e) {
                    logger.error("error processing {}", file, e);
                    Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
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

    private Object inNotNull(Module m1) {
        if (m1 == null)
        //return new Label("Не предусмотрено");
        {
            Image image = new Image("Не предусмотрено", new ThemeResource("images/cross_lines.png"));
            image.setWidth(10, Unit.PIXELS);
            image.setHeight(10, Unit.PIXELS);
            return image;
        }
        Module module = m1;
        String bgColorStyle = "";
        if (module.getColor() != 0)
            bgColorStyle = "background-color: " + String.format("#%06X", (0xFFFFFF & module.getColor())) + ";";
        String moduleHtml = "<div style='" + bgColorStyle + "width: 20px; padding: 2px 2px 2px 2px'>";
        logger.debug("moduleHtml:{}", moduleHtml);
        return new Label(moduleHtml
                + (module.getValue() != 0 ? module.getValue() + "" : "&nbsp;&nbsp;") + "</div>", ContentMode.HTML);
    }

    private class StudentSettingsButton extends Button {

        private Student student;

        public StudentSettingsButton() {
            super("Редактировать");
            this.setEnabled(false);
            this.addClickListener(event -> this.getUI().addWindow(new AccountWindow(student, userDao::saveUser)));
        }

        public Student getStudent() {
            return student;
        }

        public void setStudent(Student student) {
            this.setEnabled(student != null);
            this.student = student;
        }
    }
}

