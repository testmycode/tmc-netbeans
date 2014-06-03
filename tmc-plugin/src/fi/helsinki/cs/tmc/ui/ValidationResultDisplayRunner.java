package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;

import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public final class ValidationResultDisplayRunner implements Runnable {

    private ValidationResult result;

    @Override
    public void run() {

        TopComponent window = WindowManager.getDefault().findTopComponent("TestResultWindow");

        if (window instanceof TestResultWindow) {
            ((TestResultWindow) window).setValidationResult(result);
        } else {
            throw new IllegalStateException("No TestResultWindow in WindowManager registry.");
        }
    }

    public void setValidationResult(final ValidationResult result) {
        this.result = result;
    }
}
