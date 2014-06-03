package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import org.openide.windows.WindowManager;

public final class ValidationResultDisplayer {

    private static ValidationResultDisplayer instance;

    private final ValidationResultDisplayRunner r = new ValidationResultDisplayRunner();

    private ValidationResultDisplayer() {}

    public static ValidationResultDisplayer getInstance() {

        if (instance == null) {
            instance = new ValidationResultDisplayer();
        }

        return instance;
    }

    public void showValidationResult(final ValidationResult result) {

        r.setValidationResult(result);
        WindowManager.getDefault().invokeWhenUIReady(r);
    }
}
