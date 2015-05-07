package ru.stankin.mj.view;

import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.event.FieldEvents;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.Module;
import ru.stankin.mj.model.ModuleJournalUploader;
import ru.stankin.mj.model.Student;
import ru.stankin.mj.model.Subject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nickl on 03.05.15.
 */
public class RatingCalculationTable extends MarksTable {

    private static final Logger logger = LogManager.getLogger(RatingCalculationTable.class);
    private Label ratingLabel;

    public RatingCalculationTable(Student student) {
        this.fillMarks(student);
    }

    @Override
    protected Object drawModuleMark(Module module) {
        if (module == null) {
            return super.drawModuleMark(module);
        }

        if (module.getSubject().getTitle().equals(ModuleJournalUploader.ACCOUMULATED_RATING))
            return super.drawModuleMark(module);
        if (module.getSubject().getTitle().equals(ModuleJournalUploader.RATING)) {
            ratingLabel = new Label(
                    (module.getValue() != 0 ? module.getValue() + "" : "&nbsp;&nbsp;"), ContentMode.HTML
            );
            updateRating();
            return ratingLabel;
        } else {

            TextField textField = new TextField("", module.getValue() + "");
            textField.setImmediate(true);
            textField.setBuffered(false);
            textField.setConverter(new StringToIntegerConverter());
            textField.addTextChangeListener(new FieldEvents.TextChangeListener() {
                @Override
                public void textChange(FieldEvents.TextChangeEvent event) {
                    String text = event.getText();
                    try {
                        module.setValue(new Integer(text));
                        //logger.debug("valuechanged:" + event + " " + module.getValue());
                        updateRating();
                    } catch (NumberFormatException e) {
                        textField.setValue(module.getValue() + "");
                    }
                }
            });
            return textField;
        }
    }


    private static Map<String, Double> marksFactor = new HashMap<>();

    static {
        marksFactor.put("М1", 3.);
        marksFactor.put("М2", 2.);
        marksFactor.put("К", 5.);
        marksFactor.put("З", 5.);
        marksFactor.put("Э", 7.);
    }

    private void updateRating() {

        double total = 0.0;

        for (Map.Entry<Subject, Map<String, Module>> subj : this.modulesGrouped.entrySet()) {
            Collection<Module> modules = subj.getValue().values();

            double subjsum = 0;
            for (Module module : modules) {
                if (module.getValue() < 25) {
                    ratingLabel.setValue("");
                    return;
                }

                subjsum += module.getValue() * marksFactor.get(module.getNum());
            }

            subjsum = subjsum / modules.stream().mapToDouble(m -> marksFactor.get(m.getNum())).sum();

            total += subjsum * subj.getKey().getFactor();
        }

        total = total / this.modulesGrouped.keySet().stream().mapToDouble(Subject::getFactor).sum();

        ratingLabel.setValue("<b>" + total + "</b>");

    }

}



