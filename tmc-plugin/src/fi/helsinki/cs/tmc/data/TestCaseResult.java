package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.testrunner.CaughtException;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import static fi.helsinki.cs.tmc.testrunner.TestCase.Status.*;
import org.netbeans.api.annotations.common.CheckForNull;

public class TestCaseResult {

    private String name;
    private boolean successful;
    private String message;
    private CaughtException exception;
    private String detailedMessage;
    private boolean valgrindFailed;

    public TestCaseResult() {
    }

    public TestCaseResult(String name, boolean successful, String message) {
        this.name = name;
        this.successful = successful;
        this.message = message;
    }

    public TestCaseResult(String name, boolean successful, String message, String valgrindTrace, boolean valgrindFailed) {
        this(name, successful, message);
        this.detailedMessage = valgrindTrace;
        this.valgrindFailed = valgrindFailed;
    }

    public String getName() {
        return name;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @CheckForNull
    public String getMessage() {
        return message;
    }

    @CheckForNull
    public CaughtException getException() {
        return exception;
    }

    @CheckForNull
    public String getDetailedMessage() {
        return detailedMessage;
    }

    public boolean getValgrindFailed() {
        return this.valgrindFailed;
    }

    /**
     * Creates a TestCaseResult from a TestCase probably returned by a local run of tmc-junit-runner.
     */
    public static TestCaseResult fromTestCaseRecord(TestCase tc) {
        TestCaseResult tcr = new TestCaseResult();
        tcr.name = tc.className + " " + tc.methodName;
        tcr.successful = (tc.status == PASSED);
        tcr.message = tc.message;
        tcr.exception = tc.exception;
        return tcr;
    }
}
