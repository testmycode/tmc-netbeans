package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import fi.helsinki.cs.tmc.data.Exercise.ValgrindStrategy;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class CTestCase {

    private static final Logger log = Logger.getLogger(CTestCase.class.getName());

    private String name;
    private String result;
    private String message;
    private String points;
    private String valgrindTrace;
    private ValgrindStrategy valgrindStrategy;
    private boolean checkedForMemoryLeaks;
    private int maxBytesAllocated = -1;

    public CTestCase(String name, String result, String message, String points, String valgrindTrace, ValgrindStrategy valgrindStrategy) {
        this(name);
        this.result = result;
        this.message = message;
        this.points = points;
        this.valgrindTrace = valgrindTrace;
        this.valgrindStrategy = valgrindStrategy;
        this.checkedForMemoryLeaks = false;
    }

    public CTestCase(String name, String result, String message, ValgrindStrategy valgrindStrategy) {
        this(name, result, message, null, null, valgrindStrategy);
    }

    public CTestCase(String name) {
        this.name = name;
    }

    private boolean failedDueToValgrind(String valgrindTrace) {
        if (ValgrindStrategy.FAIL == valgrindStrategy) {
            return StringUtils.isNotBlank(valgrindTrace);
        }
        return false;
    }

    public TestCaseResult createTestCaseResult() {
        String msg = message;

        boolean valgrindFailed = failedDueToValgrind(valgrindTrace);
        boolean resultsSuccessful = result.equals("success");
        boolean successful = resultsSuccessful && !valgrindFailed;
        boolean failedOnlyBecauseOfValgrind = resultsSuccessful && valgrindFailed;
        if (failedOnlyBecauseOfValgrind) {
            msg += " - Failed due to errors in valgrind log; see log below. Try submitting to server, some leaks might be platform dependent";
        }


        return new TestCaseResult(name, successful, msg, valgrindTrace, failedOnlyBecauseOfValgrind);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPoints() {
        return this.points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getValgrindTrace() {
        return valgrindTrace;
    }

    public void setValgrindTrace(String valgrindTrace) {
        this.valgrindTrace = valgrindTrace;
    }

    public boolean isCheckedForMemoryLeaks() {
        return checkedForMemoryLeaks;
    }

    public boolean isCheckedForMemoryUsage() {
        return this.maxBytesAllocated >= 0;
    }

    public int getMaxBytesAllocated() {
        return maxBytesAllocated;
    }

    public void setMaxBytesAllocated(int maxAllocations) {
        this.maxBytesAllocated = maxAllocations;
    }

    public void setCheckedForMemoryLeaks(boolean checkedForMemoryLeaks) {
        this.checkedForMemoryLeaks = checkedForMemoryLeaks;
    }

}
