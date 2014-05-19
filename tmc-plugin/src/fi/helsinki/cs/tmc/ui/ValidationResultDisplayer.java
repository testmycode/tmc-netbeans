package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;

public final class ValidationResultDisplayer {

    private static ValidationResultDisplayer instance;

    private ValidationResultDisplayer() {}

    public static ValidationResultDisplayer getInstance() {

        if (instance == null) {
            instance = new ValidationResultDisplayer();
        }

        return instance;
    }

    public void showValidationResult(final ValidationResult result) {

        TestResultWindow.get().setValidationResult(result);
    }
}
