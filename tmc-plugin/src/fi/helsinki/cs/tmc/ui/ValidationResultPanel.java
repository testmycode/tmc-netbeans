package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.ValidationResult;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;

public final class ValidationResultPanel extends JPanel {

    public ValidationResultPanel() {

        this.setLayout(new GridLayout(0, 1));
    }

    public void setValidationResult(final ValidationResult result) {

        this.removeAll();

        for (Entry<File, List<ValidationError>> entry : result.getValidationErrors().entrySet()) {

            final File file = entry.getKey();
            final List<ValidationError> errors = entry.getValue();

            StringBuilder builder = new StringBuilder();

            for (ValidationError error : errors) {

                builder.append("Line ");
                builder.append(error.getLine());
                builder.append(": ");
                builder.append(error.getMessage());
                builder.append("\n");
            }

            this.add(new ResultCell(new Color(0xFFD000), file.getName(), builder.toString(), null));
        }
    }
}
