package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.resources.LocalizedMessage;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.netbeans.api.java.classpath.GlobalPathRegistry;

import org.openide.filesystems.FileObject;

public final class ValidationResultBuilder {

    private ValidationResultBuilder() {}

    public static List<ResultCell> buildCells(final ValidationResult validationResult) {

        final List<ResultCell> resultCells = new ArrayList<ResultCell>();

        for (Map.Entry<File, List<ValidationError>> entry : validationResult.getValidationErrors().entrySet()) {

            final File file = entry.getKey();
            final List<ValidationError> errors = entry.getValue();

            final StringBuilder builder = new StringBuilder();

            for (ValidationError error : errors) {

                builder.append(String.format("%1$-10s", LocalizedMessage.getFormattedMessage("label.line", error.getLine())))
                        .append(" ")
                        .append(error.getMessage())
                        .append("\n");
            }

            final FileObject fileObject = GlobalPathRegistry.getDefault().findResource(file.toString());

            if (fileObject == null) {

                resultCells.add(new ResultCell(new Color(0xFFD000),
                                               Color.DARK_GRAY,
                                               file.getName(),
                                               builder.toString(),
                                               null));
            } else {

                resultCells.add(new ResultCell(new Color(0xFFD000),
                                               Color.DARK_GRAY,
                                               fileObject,
                                               builder.toString(),
                                               null));
            }
        }

        return resultCells;
    }
}
