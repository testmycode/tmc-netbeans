package fi.helsinki.cs.tmc.data;

import java.util.List;

public class TestRunResult {
    private final List<TestCaseResult> testCaseResults;

    public TestRunResult(List<TestCaseResult> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }
}
