package ru.stankin.mj.view;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.filter.UnsupportedFilterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.StudentsStorage;
import ru.stankin.mj.model.Student;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nickl on 31.01.15.
 */
public class StudentsContainer extends AbstractContainer implements Container, Container.ItemSetChangeNotifier/*, Container.Filterable,
        Container.Indexed, Container.Sortable  */
{
    private static final Logger logger = LogManager.getLogger(StudentsContainer.class);

    StudentsStorage storage;

    List<String> properiesNames = Arrays.asList("id","Группа","Фамилия","ИО","Логин");

    private List<Integer> idis;
    private Set<Integer> idisSet;

    private String filter;

    class StudentItem implements Item{

        //private Student student;

        private HashMap<String, Property> props = new HashMap<>();

        public StudentItem(Student student) {
            //this.student = student;
            props.put("id", new ObjectProperty<>(student.id));
            props.put("Группа", new ObjectProperty<>(student.stgroup));
            props.put("Фамилия", new ObjectProperty<>(student.surname));
            props.put("ИО", new ObjectProperty<>(student.initials));
            props.put("Логин", new ObjectProperty<>(student.cardid != null? student.cardid : ""));
            //props.put("Пароль", new ObjectProperty<>(student.password  != null? student.password : ""));

        }

        @Override
        public Property getItemProperty(Object id) {
            //logger.debug("getItemProperty {}", id);
            return props.get(id);
        }

        @Override
        public Collection<?> getItemPropertyIds() {
            return props.keySet();
        }

        @Override
        public boolean addItemProperty(Object id, Property property) throws UnsupportedOperationException {
            logger.debug("addItemProperty unsupported");
            throw new UnsupportedFilterException("addItemProperty");
        }

        @Override
        public boolean removeItemProperty(Object id) throws UnsupportedOperationException {
            logger.debug("removeItemProperty unsupported");
            throw new UnsupportedFilterException("removeItemProperty");
        }
    }

    private StudentItem sampleItem = new StudentItem(new Student("","",""));

    public StudentsContainer(StudentsStorage storage) {
        this.storage = storage;
        updateData();
    }

    private void updateData() {
        try (Stream<Integer> idStream = this.storage.getStudentsFiltred(filter).map(s -> s.id)) {
            idis = idStream.collect(Collectors.toList());
            idisSet = idis.stream().collect(Collectors.toSet());
            fireItemSetChange();
        }
    }

    @Override
    public Item getItem(Object itemId) {
        return new StudentItem(storage.getStudentById((Integer) itemId, null));
    }

    @Override
    public Collection<?> getContainerPropertyIds() {
        return properiesNames.subList(1, properiesNames.size());
    }

    @Override
    public Collection<?> getItemIds() {
        //logger.debug("getItemIds called");
        return idis;
    }

    @Override
    public Property getContainerProperty(Object itemId, Object propertyId) {
        return getItem(itemId).getItemProperty(propertyId);
    }

    @Override
    public Class<?> getType(Object propertyId) {
       return sampleItem.getItemProperty(propertyId).getType();
    }

    @Override
    public int size() {
        return idis.size();
    }

    @Override
    public boolean containsId(Object itemId) {
        boolean result = idisSet.contains(itemId);
        //logger.debug("containsId {} = {}", itemId, result);
        return result;
    }

    @Override
    public Item addItem(Object itemId) throws UnsupportedOperationException {
        return null;
    }

    @Override
    public Object addItem() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        return false;
    }

    @Override
    public boolean addContainerProperty(Object propertyId, Class<?> type, Object defaultValue) throws
            UnsupportedOperationException {
        //logger.debug("addContainerProperty {}", propertyId);
        return false;
    }

    @Override
    public boolean removeContainerProperty(Object propertyId) throws UnsupportedOperationException {
        //logger.debug("removeContainerProperty {}", propertyId);
        return false;
    }

    @Override
    public boolean removeAllItems() throws UnsupportedOperationException {
        return false;
    }

    @Override
    public void removeListener(ItemSetChangeListener listener) {
        super.removeListener(listener);
    }

    @Override
    public void addItemSetChangeListener(ItemSetChangeListener listener) {
        super.addItemSetChangeListener(listener);
    }

    @Override
    public void addListener(ItemSetChangeListener listener) {
        super.addListener(listener);
    }

    @Override
    public void removeItemSetChangeListener(ItemSetChangeListener listener) {
        super.removeItemSetChangeListener(listener);
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        //logger.debug("update Filters {}", filter);
        this.filter = filter;
        updateData();
    }
}
