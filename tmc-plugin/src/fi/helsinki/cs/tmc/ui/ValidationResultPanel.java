package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.validation.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;

import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.filesystems.FileObject;

public final class ValidationResultPanel extends JPanel {

    private final GridBagConstraints constraints = new GridBagConstraints();

    public ValidationResultPanel() {

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.insets.top = 5;

        this.setLayout(new GridBagLayout());
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

            final FileObject fileObject = GlobalPathRegistry.getDefault().findResource(file.toString());

            if (fileObject == null) {

                this.add(new ResultCell(new Color(0xFFD000),
                         Color.DARK_GRAY,
                         file.getName(),
                         builder.toString(),
                         null),
                         constraints);
            } else {

                this.add(new ResultCell(new Color(0xFFD000),
                         Color.DARK_GRAY,
                         fileObject,
                         builder.toString(),
                         null),
                         constraints);
            }

        }
    }
}
