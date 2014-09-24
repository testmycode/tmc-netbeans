package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Exercise.ValgrindStrategy;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.data.serialization.cresultparser.CTestResultParser;
import fi.helsinki.cs.tmc.testrunner.StackTraceSerializer;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class TestResultParser {
    public TestRunResult parseTestResults(File resultsFile) throws IOException {
        String resultsJson = FileUtils.readFileToString(resultsFile, "UTF-8");
        return parseTestResults(resultsJson);
    }

    public TestRunResult parseTestResults(String resultsJson) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
            .create();

        TestCaseList testCaseRecords = gson.fromJson(resultsJson, TestCaseList.class);
        if (testCaseRecords == null) {
            throw new IllegalArgumentException("Empty result from test runner");
        }

        List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();
        for (TestCase tc : testCaseRecords) {
            testCaseResults.add(TestCaseResult.fromTestCaseRecord(tc));
        }
        return new TestRunResult(testCaseResults);
    }

    public TestRunResult parseCTestResults(File resultsFile, File valgrindLog, ValgrindStrategy valgrindStrategy) throws Exception {
        // CTestResultParser could use refactoring. Duplicates parseTestResults and is kinda messy.
        CTestResultParser parser = new CTestResultParser(resultsFile, valgrindLog, valgrindStrategy);
        parser.parseTestOutput();
        return new TestRunResult(parser.getTestCaseResults());
    }
}
