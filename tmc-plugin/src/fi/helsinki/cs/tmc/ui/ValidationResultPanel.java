package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.ValidationResult;

import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JTextArea;

public final class ValidationResultPanel extends JPanel {

    public ValidationResultPanel() {

        this.setLayout(new GridLayout(0, 1));
    }

    public void setValidationResult(final ValidationResult result) {

        this.removeAll();

        JTextArea area = new JTextArea();
        area.setEditable(false);

        for (Entry<File, List<ValidationError>> entry : result.getValidationErrors().entrySet()) {

            area.append("File: " + entry.getKey().getName() + ", errors: " + entry.getValue().size() + "\n");

            for (ValidationError error : entry.getValue()) {
                area.append("  " + error.getMessage() + "\n");
            }

            area.append("\n");
        }

        this.add(area);
    }
}
