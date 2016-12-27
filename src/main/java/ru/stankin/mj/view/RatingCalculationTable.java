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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by nickl on 03.05.15.
 */
public class RatingCalculationTable extends MarksTable {

    private static final Logger log = LogManager.getLogger(RatingCalculationTable.class);
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
        } else if (module.disabled()) {
            return new Label("-");
        } else {

            return new ModuleField(module, event -> updateRating());
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
        double factorssum = 0.0;

        for (Map.Entry<Subject, Map<String, Module>> subj : this.modulesGrouped.entrySet()) {
            Collection<Module> modules = subj.getValue().values().stream().filter(m -> !m.disabled()).collect(Collectors.toList());

            if(modules.isEmpty())
                continue;

            double subjsum = 0;
            for (Module module : modules) {
                if (module.getValue() < 25) {
                    ratingLabel.setValue("");
                    return;
                }

                subjsum += module.getValue() * marksFactor.get(module.getNum());
            }

            subjsum = subjsum / modules.stream().mapToDouble(m -> marksFactor.get(m.getNum())).sum();

            double factor = subj.getKey().getFactor();
            total += subjsum * factor;
            factorssum += factor;
        }

        total = total / factorssum;

        ratingLabel.setValue("<b>" + total + "</b>");

    }

}

class ModuleField extends TextField{

    private static final Logger logger = LogManager.getLogger(ModuleField.class);


    Module module;

    FieldEvents.TextChangeListener textChangeListener;

    public ModuleField(Module module, FieldEvents.TextChangeListener textChangeListener) {
        super("", module.getValue()+"");
        this.module = module;
        this.textChangeListener = textChangeListener;

        styleModule();
        this.setImmediate(true);
        this.setBuffered(false);
        this.setConverter(new BlanckAwareToIntegerConverter());
        this.addTextChangeListener(new FieldEvents.TextChangeListener() {
            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                String text = event.getText();
                if (text.isEmpty()) {
                    module.setValue(0);
                    textChangeListener.textChange(event);
                } else
                    try {
                        module.setValue(new Integer(text));
                        //logger.debug("valuechanged:" + event + " " + module.getValue());
                        textChangeListener.textChange(event);
                    } catch (NumberFormatException e) {
                        logger.debug("returning value:" + module.getValue());
                        ((TextField) ModuleField.this).setValue(module.getValue() + "");
                    }

                styleModule();

            }
        });


    }

    private void styleModule() {
        if(module.getValue() < 25)
            this.addStyleName("missingmodule");
        else
            this.removeStyleName("missingmodule");
    }

    private static class BlanckAwareToIntegerConverter extends StringToIntegerConverter {
        @Override
        protected Number convertToNumber(String value, Class<? extends Number> targetType, Locale locale) throws ConversionException {
            Number number = super.convertToNumber(value, targetType, locale);
            return number != null ? number : 0;
        }
    }
}



