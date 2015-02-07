package ru.stankin.mj;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.easyuploads.FileBuffer;
import org.vaadin.easyuploads.MultiFileUpload;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.*;


@CDIView("")
public class MainView extends CustomComponent implements View {

    private static final Logger logger = LogManager.getLogger(MainView.class);

    @Inject
    private UserInfo user;

    @Inject
    private Storage storage;

    @Inject
    private ModuleJournalUploader moduleJournalUploader;

    @Inject
    private ExecutorService ecs;

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
        Button settings = new Button("Аккаунт");
        settings.setEnabled(false);
        content.addComponent(settings);
        content.setComponentAlignment(settings, Alignment.TOP_RIGHT);
        Button exit = new Button("Выход");
        exit.addClickListener(event1 -> {
                    user.setUser(null);
            this.getUI().getPage().reload();
        });
        content.addComponent(exit);
        content.setComponentAlignment(exit, Alignment.TOP_RIGHT);

        pael1.setContent(content);

       // pael1.setWidth(100, Unit.PERCENTAGE);
      //  pael1.setHeight(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(pael1);

        MultiFileUpload marksUpload = createUpload();
        HorizontalLayout uploadAndGrids = new HorizontalLayout();
        //uploadAndGrids.setMargin(true);
        VerticalLayout uploads = new VerticalLayout();
        uploads.setHeight(100, Unit.PERCENTAGE);
        uploads.setMargin(true);
        uploads.addComponent(marksUpload);
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
        verticalLayout.addComponent(uploadAndGrids);
        verticalLayout.setExpandRatio(uploadAndGrids, 1);
        verticalLayout.setSizeFull();
        setCompositionRoot(verticalLayout);

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
        students.addContainerProperty("Пароль", String.class, null);
        students.setColumnWidth("Пароль", 60);

        students.setEditable(true);
        students.setSelectable(true);
        students.setTableFieldFactory(new TableFieldFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {

                if (propertyId.equals("Логин") || propertyId.equals("Пароль")) {
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


        Table marks = new Table();

        marks.addContainerProperty("Предмет", String.class, null);
        marks.setColumnWidth("Предмет", 200);
        marks.addContainerProperty("М1", Label.class, null);
        marks.setColumnWidth("М1", 30);
        marks.addContainerProperty("М2", Label.class, null);
        marks.setColumnWidth("М2", 30);

        marks.setSizeFull();
        marks.setWidth(100, Unit.PERCENTAGE);

        Label label = new Label("", ContentMode.HTML);
        students.addValueChangeListener(event1 -> {
            logger.debug("selection:{}",event1);
            //logger.debug("stacktacer:{}",new Exception("stacktrace"));
            if(event1.getProperty() == null || event1.getProperty().getValue() == null)
                return;
            Student student = storage.getStudentById((Integer) event1.getProperty().getValue(), true);
            label.setValue("<b>" + student.surname + " " + student.initials + "</b>");

            marks.removeAllItems();

            for (int i = 0; i < student.getModules().size(); i += 2) {
                String subject = student.getModules().get(i).subject;
                int m1 = student.getModules().get(i).value;
                int m2 = student.getModules().get(i + 1).value;
                marks.addItem(new Object[]{subject, inNotNull(m1), inNotNull(m2)}, i);
            }

        });

        GridLayout grid = new GridLayout(2, 3);
        grid.setHeight(100, Unit.PERCENTAGE);
        grid.setWidth(100, Unit.PERCENTAGE);
        //grid.addComponent(upload, 0, 0);
        grid.addComponent(searchForm, 0, 0);
        grid.addComponent(students, 0 , 1);
        grid.addComponent(label, 1 , 0);
        grid.addComponent(marks, 1 , 1);
        grid.setSpacing(true);
        grid.setRowExpandRatio(0,0);
        grid.setRowExpandRatio(1,1);
        grid.setColumnExpandRatio(0,1);
        grid.setColumnExpandRatio(1,1);

        grid.setMargin(true);
        return /*new Panel(*/grid/*)*/;
    }

    private MultiFileUpload createUpload() {
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
                    moduleJournalUploader.processIncomingDate(is);
                    is.close();
                    Notification.show(msg);
                }
                catch (Exception e){
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
        upload.setRootDirectory(Files.createTempDir().toString());
        return upload;
    }

    private Object inNotNull(int m1) {
        return new Label("<span style='background-color: blue; padding: 2px 2px 2px 2px'>"+(m1 != 0 ? m1 + "" : "")+"</span>", ContentMode.HTML);
    }

}

