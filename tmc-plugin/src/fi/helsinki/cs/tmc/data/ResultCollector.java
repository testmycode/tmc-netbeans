package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.stylerunner.validation.Strategy;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.ui.TestResultWindow;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Course;

import java.util.List;

/**
 * Waits for test and validation results and shows the result view only when
 * both have become available.
 */
public final class ResultCollector {

    private final Exercise exercise;
    private List<TestCaseResult> testCaseResults;
    private ValidationResult validationResults;

    private boolean testCaseResultsSet = false;
    private boolean validationResultsSet = false;

    private boolean isReturnable;
    private Runnable submissionCallback;

    public ResultCollector(Exercise exercise) {
        this.exercise = exercise;
    }

    public void setValidationResult(final ValidationResult result) {

        this.validationResults = result;
        this.validationResultsSet = true;

        showResultsIfReady();
    }

    public void setTestCaseResults(final List<TestCaseResult> results) {

        this.testCaseResults = results;
        this.testCaseResultsSet = true;

        showResultsIfReady();
    }

    private void showResultsIfReady() {

        boolean ready = testCaseResultsSet && validationResultsSet;
        if (ready) {
            TestResultWindow.get()
                    .showResults(
                            exercise,
                            testCaseResults,
                            validationResults,
                            submissionCallback,
                            isSubmittable());
        }
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

        if (validationResults == null || validationResults.getStrategy() != Strategy.FAIL) {
            return isReturnable;
        }

        if (!validationResults.getValidationErrors().isEmpty()) {
            return false;
        }

        return isReturnable;
    }
}
