package fi.helsinki.cs.tmc.testHandler.testResultsHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.testrunner.StackTraceSerializer;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractTestResultsHandler {

    protected TestResultDisplayer resultDisplayer;
    protected ConvenientDialogDisplayer dialogDisplayer;
    protected ExerciseSubmitter exerciseSubmitter;
    private static final Logger log = Logger.getLogger(AbstractTestResultsHandler.class.getName());
    protected ProjectMediator projectMediator;
    protected CourseDb courseDb;

    public AbstractTestResultsHandler() {
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.exerciseSubmitter = new ExerciseSubmitter();
        this.projectMediator = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
    }

    abstract public void handle(TmcProjectInfo projectInfo, File resultsFile);

    protected List<TestCaseResult> parseTestResults(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
                .create();

        TestCaseList testCaseRecords = gson.fromJson(json, TestCaseList.class);
        if (testCaseRecords == null) {
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

    protected ProjectMediator getProjectMediator() {
        return this.projectMediator;
    }

    protected CourseDb getCourseDb() {
        return this.courseDb;
    }

    protected boolean isReturnable(TmcProjectInfo projectInfo) {
        Exercise ex = getProjectMediator().tryGetExerciseForProject(getProjectMediator().wrapProject(projectInfo.getProject()), getCourseDb());
        return ex.isReturnable();
    }
}
