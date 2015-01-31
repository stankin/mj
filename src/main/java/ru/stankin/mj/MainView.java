package ru.stankin.mj;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.ModuleJournal;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;

import javax.inject.Inject;
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

        logger.debug("entered");

        setHeight("100%");
        VerticalLayout verticalLayout = new VerticalLayout();

//        verticalLayout.setWidth("100%");
//        verticalLayout.setHeight("100%");
        Panel pael1 = new Panel();
        HorizontalLayout content = new HorizontalLayout();
         content.setWidth("100%");
        content.addComponent(new Button("77"));

        Button exit = new Button("exit");
        content.addComponent(exit);
        content.setComponentAlignment(exit, Alignment.TOP_RIGHT);

        pael1.setContent(content);

       // pael1.setWidth(100, Unit.PERCENTAGE);
      //  pael1.setHeight(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(pael1);

        Component c = buildGrids();
        //TextArea c = new TextArea();
        c.setHeight(100, Unit.PERCENTAGE);
        verticalLayout.addComponent(c);
        verticalLayout.setExpandRatio(c,1);
        verticalLayout.setSizeFull();
        setCompositionRoot(verticalLayout);

    }

    private Component buildGrids() {


        //verticalLayout.setMargin(true);
        FileReceiver uploadReceiver = new FileReceiver(this, moduleJournal, ecs);
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
                    field.addValueChangeListener(event -> {
                        logger.debug("Property.ValueChangeEvent:" + event);
                        logger.debug("itemId:" + itemId);
                    });
                    return field;
                } else
                    return null;

            }
        });


        TextField find = new TextField();
        Layout formLayout = new HorizontalLayout(new Label("Поиск"), find);

        students.setContainerDataSource(new StudentsContainer(storage));

//        find.addTextChangeListener(event1 -> {
//            students.removeAllItems();
//            String text = event1.getText();
//            if (text.length() > 4) {
//                storage.getStudentsFiltred(text).forEach(student -> {
//                    Object[] cells = {student.group, student.surname, student.initials, student.login, student
//                            .password};
//                    students.addItem(cells, student.id);
//                });
////                    for (int i = 0; i < students.size(); i++) {
////                        Student student = students.get(i);
////
////                    }
//            }
//
//        });


        Table marks = new Table();

        marks.addContainerProperty("Предмет", String.class, null);
        marks.setColumnWidth("Предмет", 200);
        marks.addContainerProperty("М1", Label.class, null);
        marks.setColumnWidth("М1", 30);
        marks.addContainerProperty("М2", Label.class, null);
        marks.setColumnWidth("М2", 30);

        marks.setSizeFull();
        marks.setWidth(400, Unit.PIXELS);

        Label label = new Label("", ContentMode.HTML);
        students.addValueChangeListener(event1 -> {
            logger.debug("selection:{}",event1);
            logger.debug("stacktacer:{}",new Exception("stacktrace"));
            if(event1.getProperty() == null || event1.getProperty().getValue() == null)
                return;
            Student student = storage.getStudentById((Integer) event1.getProperty().getValue());
            label.setValue("<b>" + student.surname + " " + student.initials + "</b>");

            marks.removeAllItems();

            for (int i = 0; i < student.modules.size(); i += 2) {
                String subject = student.modules.get(i).subject;
                int m1 = student.modules.get(i).value;
                int m2 = student.modules.get(i + 1).value;
                marks.addItem(new Object[]{subject, inNotNull(m1), inNotNull(m2)}, i);
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

        grid.setMargin(true);
        return /*new Panel(*/grid/*)*/;
    }

    private Object inNotNull(int m1) {
        return new Label("<span style='background-color: blue; padding: 2px 2px 2px 2px'>"+(m1 != 0 ? m1 + "" : "")+"</span>", ContentMode.HTML);
    }

}

