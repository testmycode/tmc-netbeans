package fi.helsinki.cs.tmc.testHandler.testResultsHandler;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseListUtils;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class JavaTestResultsHandler extends AbstractTestResultsHandler {

    private static final Logger log = Logger.getLogger(JavaTestResultsHandler.class.getName());

    @Override
    public void handle(final TmcProjectInfo projectInfo, File resultsFile) {
        List<TestCaseResult> results;
        try {
            String resultJson = FileUtils.readFileToString(resultsFile, "UTF-8");
            results = parseTestResults(resultJson);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to read test results: {0}", ex.getStackTrace());
            dialogDisplayer.displayError("Failed to read test results", ex);
            return;
        }
        
        Exercise ex = getProjectMediator().tryGetExerciseForProject(getProjectMediator().wrapProject(projectInfo.getProject()), getCourseDb());
        boolean canSubmit = ex.isReturnable();
        resultDisplayer.showLocalRunResult(results, canSubmit, new Runnable() {
            @Override
            public void run() {
                exerciseSubmitter.performAction(projectInfo.getProject());
            }
        });
    }

    

}
