package ru.stankin.mj;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import ru.stankin.mj.model.ModuleJournal;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;


@CDIView("")
public class MainView extends CustomComponent implements View {

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

        verticalLayout.setMargin(true);
        FileReceiver uploadReceiver = new FileReceiver();
        Upload upload = new Upload("Загрузка файла", uploadReceiver);

        uploadReceiver.serve(upload);

        verticalLayout.addComponent(upload);

        Table table = new Table();
        table.setWidth(400, Unit.PIXELS);


        table.addContainerProperty("Группа", String.class, null);
        table.setColumnWidth("Группа", 60);
        table.addContainerProperty("Фамилия", String.class, null);
        table.setColumnWidth("Фамилия", 100);
        table.addContainerProperty("ИО", String.class, null);
        table.setColumnWidth("ИО", 30);
        table.addContainerProperty("Логин", String.class, "");
        table.setColumnWidth("Логин", 60);
        table.addContainerProperty("Пароль", String.class, "");
        table.setColumnWidth("Пароль", 60);

        table.setEditable(true);
        table.setSelectable(true);
        table.setTableFieldFactory(new TableFieldFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {

                if (propertyId.equals("Логин") || propertyId.equals("Пароль")) {
                    Field field = DefaultFieldFactory.get().createField(container, itemId, propertyId, uiContext);
                    field.addValueChangeListener(new Property.ValueChangeListener() {
                        @Override
                        public void valueChange(Property.ValueChangeEvent event) {

                            System.out.println("Property.ValueChangeEvent:"+event);
                            System.out.println("itemId:"+itemId);
                        }
                    });
                    return field;
                }
                else
                    return null;

            }
        });



        TextField find = new TextField();
        Layout formLayout = new HorizontalLayout(new Label("Поиск"), find);


        find.addTextChangeListener(event1 -> {
            table.removeAllItems();
            String text = event1.getText();
            if (text.length() > 4) {
                storage.getStudentsFiltred(text).forEach(student -> {
                    Object[] cells = {student.group, student.surname, student.initials, student.login, student
                            .password};
                    table.addItem(cells, null);
                });
//                    for (int i = 0; i < students.size(); i++) {
//                        Student student = students.get(i);
//
//                    }
            }

        });


        verticalLayout.addComponent(formLayout);
        verticalLayout.addComponent(table);


        //table.setContainerDataSource(new );


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

