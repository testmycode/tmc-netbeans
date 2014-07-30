package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.stylerunner.validation.Strategy;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.ui.TestResultWindow;

import java.util.List;

/**
 * Waits for test and validation results and shows the result view only when
 * both have become available.
 */
public final class ResultCollector {

    private List<TestCaseResult> testCaseResults;
    private ValidationResult validationResults;

    private boolean testCaseResultLock = true;
    private boolean validationResultLock = true;

    private boolean isReturnable;
    private Runnable submissionCallback;

    public void setValidationResult(final ValidationResult result) {

        this.validationResults = result;
        this.validationResultLock = false;

        if (!testCaseResultLock) {
            showAllResults();
        }
    }

    public void setTestCaseResults(final List<TestCaseResult> results) {

        this.testCaseResults = results;
        this.testCaseResultLock = false;

        if (!validationResultLock) {
            showAllResults();
        }
    }

    private void showAllResults() {

        TestResultWindow.get().showResults(testCaseResults, validationResults, submissionCallback, isSubmittable());
    }

    public void setSubmissionCallback(final Runnable submissionCallback) {

        this.submissionCallback = submissionCallback;
    }

    public void setReturnable(final boolean returnable) {

        isReturnable = returnable;
    }

    private boolean isSubmittable() {

        for (TestCaseResult result : testCaseResults) {

            if (!result.isSuccessful()) {
                return false;
            }
        }

        if (validationResults.getStrategy() != Strategy.FAIL) {
            return isReturnable;
        }

        if (!validationResults.getValidationErrors().isEmpty()) {
            return false;
        }

        return isReturnable;
    }
}
