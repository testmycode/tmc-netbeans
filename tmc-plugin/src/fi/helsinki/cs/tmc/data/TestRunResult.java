package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.List;

public class TestRunResult {
    private final List<TestCaseResult> testCaseResults;
    private final boolean compilationSucceeded;

    public TestRunResult(List<TestCaseResult> testCaseResults) {
        this.compilationSucceeded = true;
        this.testCaseResults = testCaseResults;
    }

    public TestRunResult(boolean compilationSucceeded) {
        this.compilationSucceeded = compilationSucceeded;
        this.testCaseResults = new ArrayList<TestCaseResult>();
    }

    public TestRunResult(List<TestCaseResult> testCaseResults, boolean compilationSucceeded) {
        this.testCaseResults = testCaseResults;
        this.compilationSucceeded = compilationSucceeded;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }
    
    public boolean getCompilationSuccess() {
        return compilationSucceeded;
    }
}
