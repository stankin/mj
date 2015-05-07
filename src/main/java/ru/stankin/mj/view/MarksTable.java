package ru.stankin.mj.view;

import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Student;
import ru.stankin.mj.model.Subject;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nickl on 01.05.15.
 */
public class MarksTable extends Table {


    protected Map<String, Module> rating;
    protected Map<String, Module> accumulatedRating;
    protected Map<Subject, Map<String, Module>> modulesGrouped;

    public MarksTable() {
        this.addContainerProperty("Предмет", AbstractComponent.class, null);
        this.setColumnWidth("Предмет", 200);
        this.addContainerProperty("М1", AbstractComponent.class, null);
        this.setColumnWidth("М1", 30);
        this.addContainerProperty("М2", AbstractComponent.class, null);
        this.setColumnWidth("М2", 30);
        this.addContainerProperty("К", AbstractComponent.class, null);
        this.setColumnWidth("К", 30);
        this.addContainerProperty("З", AbstractComponent.class, null);
        this.setColumnWidth("З", 30);
        this.addContainerProperty("Э", AbstractComponent.class, null);
        this.setColumnWidth("Э", 30);
        this.addContainerProperty("к", AbstractComponent.class, null);
        this.setColumnWidth("к", 30);
    }

    public void fillMarks(Student student) {
        removeAllItems();
        modulesGrouped = null;
        rating = null;
        accumulatedRating = null;

        if (student == null){
            return;
        }
        int size = student.getModules().size();
        //logger.debug("student has:{} modules", size);
        AtomicInteger i = new AtomicInteger(0);
        modulesGrouped = student.getModulesGrouped();
        //logger.debug("modulesGrouped:{} ", modulesGrouped);

        rating = removeByTitle(modulesGrouped, ModuleJournalUploader.RATING);
        accumulatedRating = removeByTitle(modulesGrouped, ModuleJournalUploader.ACCOUMULATED_RATING);

        modulesGrouped.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getTitle()))
                .forEach(subj -> {
                    Subject subject = subj.getKey();
                    Module m1 = subj.getValue().get("М1");
                    Module m2 = subj.getValue().get("М2");
                    Module m3 = subj.getValue().get("К");
                    Module m4 = subj.getValue().get("З");
                    Module m5 = subj.getValue().get("Э");
                    addItem(
                            new Object[]{
                                    new Label(subject.getTitle()),
                                    drawModuleMark(m1),
                                    drawModuleMark(m2),
                                    drawModuleMark(m3),
                                    drawModuleMark(m4),
                                    drawModuleMark(m5),
                                    orNoValue((subject.getFactor() != 0 ? new Label("<i>" + subject.getFactor() + "</i>", ContentMode.HTML) : null))
                            },
                            i.incrementAndGet());
                });

        addSummary(ModuleJournalUploader.RATING, rating, i);
        addSummary(ModuleJournalUploader.ACCOUMULATED_RATING, accumulatedRating, i);

    }

    private Map<String, Module> removeByTitle(Map<Subject, Map<String, Module>> modulesGrouped, String title) {
        return modulesGrouped.keySet().stream().filter(s -> s.getTitle().equals(title)).findFirst()
                .map((Subject s) -> modulesGrouped.remove(s)).orElse(null);
    }

    protected Object drawModuleMark(Module module) {
        if (module == null)
        {
            return orNoValue(null);
        }
        String bgColorStyle = "";
        if (module.getColor() != -1)
            bgColorStyle = "background-color: " + String.format("#%06X", (0xFFFFFF & module.getColor())) + ";";
        String moduleHtml = "<div style='" + bgColorStyle + "width: 20px; padding: 2px 2px 2px 2px'>";
        //logger.debug("moduleHtml:{}", moduleHtml);
        return new Label(moduleHtml
                + (module.getValue() != 0 ? module.getValue() + "" : "&nbsp;&nbsp;") + "</div>", ContentMode.HTML);
    }

    protected Object orNoValue(Object v) {
        if (v != null)
            return v;
        Image image = new Image("Не предусмотрено", new ThemeResource("images/cross_lines.png"));
        image.setWidth(10, Unit.PIXELS);
        image.setHeight(10, Unit.PIXELS);
        return image;
    }

    protected void addSummary(final String label, Map<String, Module> raiting, AtomicInteger i) {
        if (raiting != null) {
            Module m1 = raiting.get("М1");
            Module m2 = raiting.get("М2");
            Module m3 = raiting.get("К");
            Module m4 = raiting.get("З");
            Module m5 = raiting.get("Э");
            addItem(
                    new Object[]{
                            new Label("<b>" + label + "</b>", ContentMode.HTML),
                            drawModuleMark(m1),
                            drawModuleMark(m2),
                            drawModuleMark(m3),
                            drawModuleMark(m4),
                            drawModuleMark(m5),
                            orNoValue(null)
                    },
                    i.incrementAndGet());
        }
    }
}
