package fi.helsinki.cs.tmc.testRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.actions.SubmitExerciseAction;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import static fi.helsinki.cs.tmc.testRunner.AbstractExerciseTestRunner.log;
import fi.helsinki.cs.tmc.testrunner.StackTraceSerializer;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

public class TestResultsHandler {

//    protected TmcSettings settings;
//    protected CourseDb courseDb;
//    protected ProjectMediator projectMediator;
    protected TestResultDisplayer resultDisplayer;
    protected ConvenientDialogDisplayer dialogDisplayer;
    protected SubmitExerciseAction submitAction;
//    protected TmcEventBus eventBus;

    public TestResultsHandler() {
//        this.settings = TmcSettings.getDefault();
//        this.courseDb = CourseDb.getInstance();
//        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.submitAction = getSubmitExerciseActionInstance();
//        this.eventBus = TmcEventBus.getDefault();
    
    }
    
    
        public static SubmitExerciseAction getSubmitExerciseActionInstance() {
        return SubmitExerciseAction.getInstance();
//        return SubmitExerciseActionHolder.INSTANCE;
    }

//    private static class SubmitExerciseActionHolder {
//
//        private static final SubmitExerciseAction INSTANCE = new SubmitExerciseAction();
//    }
//
//    
//    protected SubmitExerciseAction getSubmitExerciseAction(){
//        return Lookup.getDefault().lookup(fi.helsinki.cs.tmc.actions.SubmitExerciseAction.class);
//    }

    public void handle(TmcProjectInfo projectInfo, File resultsFile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    protected void handleTestResults(final TmcProjectInfo projectInfo, File resultsFile) {
        List<TestCaseResult> results;
        try {
            String resultJson = FileUtils.readFileToString(resultsFile, "UTF-8");
            results = parseTestResults(resultJson);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to read test results: {0}", ex.getStackTrace());
            dialogDisplayer.displayError("Failed to read test results", ex);
            return;
        }
        boolean canSubmit = submitAction.enable(projectInfo.getProject());
        resultDisplayer.showLocalRunResult(results, canSubmit, new Runnable() {
            @Override
            public void run() {
                submitAction.performAction(projectInfo.getProject());
            }
        });
    }
    
    

    protected List<TestCaseResult> parseTestResults(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
                .create();

        TestCaseList testCaseRecords = gson.fromJson(json, TestCaseList.class);
        if (testCaseRecords
                == null) {
            String msg = "Empty result from test runner";
            log.warning(msg);
            throw new IllegalArgumentException(msg);
        }
        List<TestCaseResult> results = new ArrayList<TestCaseResult>();
        for (TestCase tc : testCaseRecords) {
            results.add(TestCaseResult.fromTestCaseRecord(tc));
        }
        return results;
    }
}
