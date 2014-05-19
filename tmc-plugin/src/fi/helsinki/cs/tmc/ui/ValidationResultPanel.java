package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.validation.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;

public final class ValidationResultPanel extends JPanel {

    public ValidationResultPanel() {

        this.setLayout(new GridLayout(0, 1, 0, 5));
    }

    public void setValidationResult(final ValidationResult result) {

        this.removeAll();

        for (Entry<File, List<ValidationError>> entry : result.getValidationErrors().entrySet()) {

            final File file = entry.getKey();
            final List<ValidationError> errors = entry.getValue();

            StringBuilder builder = new StringBuilder();

            for (ValidationError error : errors) {

                builder.append("Line ")
                       .append(error.getLine())
                       .append(": ")
                       .append(error.getMessage())
                       .append("\n");
            }

            this.add(new ResultCell(new Color(0xFFD000),
                                    Color.DARK_GRAY,
                                    file.getName(),
                                    builder.toString(),
                                    null));
        }
    }
}
