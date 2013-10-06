package fi.helsinki.cs.tmc.testHandler.testResultsHandler;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.data.serialization.cresultparser.CTestResultParser;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.io.File;
import java.util.List;

public class CTestResultsHandler extends AbstractTestResultsHandler {

    protected CTestResultParser parser;

    public CTestResultsHandler(File testResults, File valgrindOutput, File memoryOptions) {
        super();
        parser = new CTestResultParser(testResults, valgrindOutput, memoryOptions);
        try {
            parser.parseTestOutput();
        } catch (Exception e) {
            dialogDisplayer.displayError("Failed to read test results:\n" + e.getClass() + " " + e.getMessage());
        }

    }

    @Override
    public void handle(final TmcProjectInfo projectInfo, File resultsFile) {
        boolean canSubmit = isReturnable(projectInfo);

        List<TestCaseResult> results = parser.getTestCaseResults();

        resultDisplayer.showLocalRunResult(results, canSubmit,
                new Runnable() {
            @Override
            public void run() {
                exerciseSubmitter.performAction(projectInfo.getProject());
            }
        });
    }
}
