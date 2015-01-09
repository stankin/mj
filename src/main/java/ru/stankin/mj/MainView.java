package ru.stankin.mj;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.ModuleJournal;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;


@CDIView("")
public class MainView extends CustomComponent implements View {

    private static final Logger logger = LogManager.getLogger(MainView.class);

    @Inject
    private UserInfo user;

    @Inject
    private Storage storage;

    @Inject
    private ModuleJournal moduleJournal;

    @Inject
    private ExecutorService ecs;

    @Override
    public void enter(ViewChangeEvent event) {

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setWidth("100%");
        verticalLayout.setHeight("100%");

        verticalLayout.setMargin(true);
        FileReceiver uploadReceiver = new FileReceiver();
        Upload upload = new Upload("Загрузка файла", uploadReceiver);

        uploadReceiver.serve(upload);



        Table students = new Table();
        students.setWidth(400, Unit.PIXELS);
        students.setHeight(100, Unit.PERCENTAGE);


        students.addContainerProperty("Группа", String.class, null);
        students.setColumnWidth("Группа", 60);
        students.addContainerProperty("Фамилия", String.class, null);
        students.setColumnWidth("Фамилия", 100);
        students.addContainerProperty("ИО", String.class, null);
        students.setColumnWidth("ИО", 30);
        students.addContainerProperty("Логин", String.class, "");
        students.setColumnWidth("Логин", 60);
        students.addContainerProperty("Пароль", String.class, "");
        students.setColumnWidth("Пароль", 60);

        students.setEditable(true);
        students.setSelectable(true);
        students.setTableFieldFactory(new TableFieldFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {

                if (propertyId.equals("Логин") || propertyId.equals("Пароль")) {
                    Field field = DefaultFieldFactory.get().createField(container, itemId, propertyId, uiContext);
                    field.addValueChangeListener(new Property.ValueChangeListener() {
                        @Override
                        public void valueChange(Property.ValueChangeEvent event) {

                            logger.debug("Property.ValueChangeEvent:" + event);
                            logger.debug("itemId:" + itemId);
                        }
                    });
                    return field;
                } else
                    return null;

            }
        });


        TextField find = new TextField();
        Layout formLayout = new HorizontalLayout(new Label("Поиск"), find);


        find.addTextChangeListener(event1 -> {
            students.removeAllItems();
            String text = event1.getText();
            if (text.length() > 4) {
                storage.getStudentsFiltred(text).forEach(student -> {
                    Object[] cells = {student.group, student.surname, student.initials, student.login, student
                            .password};
                    students.addItem(cells, student.id);
                });
//                    for (int i = 0; i < students.size(); i++) {
//                        Student student = students.get(i);
//
//                    }
            }

        });


        Table marks = new Table();

        marks.addContainerProperty("Предмет", String.class, null);
        marks.setColumnWidth("Предмет", 200);
        marks.addContainerProperty("М1", String.class, null);
        marks.setColumnWidth("М1", 30);
        marks.addContainerProperty("М2", String.class, null);
        marks.setColumnWidth("М2", 30);
        marks.setSizeFull();
        marks.setWidth(400, Unit.PIXELS);

        Label label = new Label("", ContentMode.HTML);
        students.addValueChangeListener(event1 -> {
            //System.out.println("selection:" + event1);
            if(event1.getProperty() == null || event1.getProperty().getValue() == null)
                return;
            Student student = storage.getStudentById((Integer) event1.getProperty().getValue());
            label.setValue("<b>" + student.surname + " " + student.initials + "</b>");

            marks.removeAllItems();

            for (int i = 0; i < student.modules.size(); i += 2) {
                String subject = student.modules.get(i).subject;
                int m1 = student.modules.get(i).value;
                int m2 = student.modules.get(i + 1).value;
                marks.addItem(new Object[]{subject, m1 != 0 ? m1 + "" : "", m2 != 0 ? m2 + "" : ""}, i);
            }

        });

        GridLayout grid = new GridLayout(2, 3);
        grid.setHeight(100, Unit.PERCENTAGE);
        grid.addComponent(upload, 0 , 0);
        grid.addComponent(formLayout, 0 , 1);
        grid.addComponent(students, 0 , 2);
        grid.addComponent(label, 1 , 1);
        grid.addComponent(marks, 1 , 2);
        grid.setSpacing(true);
        grid.setRowExpandRatio(0,0);
        grid.setRowExpandRatio(1,0);
        grid.setRowExpandRatio(2,1);


        verticalLayout.addComponent(grid);


        setCompositionRoot(verticalLayout);
    }

    class FileReceiver implements Upload.Receiver {

        private Future<String> parsing = null;

        private Upload upload;

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            parsing = null;
            PipedInputStream pipedInputStream = new PipedInputStream(1024 * 100);
            try {
                parsing = ecs.submit(() -> {
                    try {
                        moduleJournal.processIncomingDate(pipedInputStream);
                        parsing = null;
                        return null;
                    } catch (Exception e) {
                        try {
                            pipedInputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return e.getMessage();
                    }
                });
                return new PipedOutputStream(pipedInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//            try {
//                return filesStorage.createNew(filename);
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }
        }

        public void serve(Upload upload) {
            this.upload = upload;

            upload.addSucceededListener(
                    event1 -> showUploadSuccess(event1, parsing)
            );

            upload.addFailedListener(e -> {
                showUploadError(parsing);
            });


        }

        private void showUploadSuccess(Upload.SucceededEvent event1, Future<String> future) {
            if (getStoredError(future) == null)
                new Notification("Файл " + event1.getFilename() + " загружен", Notification.Type
                        .TRAY_NOTIFICATION).show(getUI().getPage());
            else
                showUploadError(future);
        }

        private void showUploadError(Future<String> future) {
            String storedError = getStoredError(future);
            Notification.show(
                    "Ошибка загрузки файла",
                    storedError,
                    Notification.Type.ERROR_MESSAGE);
        }

        private String getStoredError(Future<String> parsing) {
            if (parsing == null)
                return null;
            try {
                return parsing.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return e.getMessage();
            } catch (ExecutionException e) {
                return e.getMessage();
            } catch (TimeoutException e) {
                return e.getMessage();
            }

        }
    }

}

