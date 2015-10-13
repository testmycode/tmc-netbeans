package fi.helsinki.cs.tmc.runners;

import static fi.helsinki.cs.tmc.langs.domain.RunResult.Status.COMPILE_FAILED;
import static java.util.logging.Level.INFO;

import fi.helsinki.cs.tmc.actions.ServerErrorHelper;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.netbeans.api.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TestRunHandler {

    private static final Logger log = Logger.getLogger(TestRunHandler.class.getName());

    public static class InvokedEvent implements TmcEvent {

        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    private final ProjectMediator projectMediator;
    private final ConvenientDialogDisplayer dialogDisplayer;
    private final TmcEventBus eventBus;
    private final TestResultDisplayer resultDisplayer;
    private final ExerciseSubmitter exerciseSubmitter;
    private final CourseDb courseDb;

    public TestRunHandler() {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.exerciseSubmitter = new ExerciseSubmitter();
        this.courseDb = CourseDb.getInstance();
    }

    public void performAction(final ResultCollector resultCollector, Project... projects) {
        projectMediator.saveAllFiles();
        for (final Project project : projects) {
            final TmcProjectInfo projectInfo = projectMediator.wrapProject(project);
            eventBus.post(new InvokedEvent(projectInfo));
            BgTaskListener bgTaskListener
                    = new BgTaskListener<RunResult>() {
                        @Override
                        public void bgTaskReady(RunResult result) {
                            if (result.status == COMPILE_FAILED) {
                                dialogDisplayer.displayError("The code did not compile.");
                                return;
                            }
                            Exercise ex = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
                            boolean canSubmit = ex.isReturnable();
                            resultDisplayer.showLocalRunResult(
                                    testResultsToTestCaseResults(result.testResults),
                                    canSubmit,
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            exerciseSubmitter.performAction(
                                                    projectInfo.getProject());
                                        }
                                    },
                                    resultCollector);
                        }

                        @Override
                        public void bgTaskFailed(Throwable ex) {
                            log.log(
                                    INFO,
                                    "performAction of TestRunHandler failed with message: {0}, \ntrace: {1}",
                                    new Object[]{
                                        ex.getMessage(), Throwables.getStackTraceAsString(ex)
                                    });
                            String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                            if (!Strings.isNullOrEmpty(msg)) {
                                dialogDisplayer.displayError(
                                        "Failed to run the tests: " + ex.getMessage());
                            }
                        }

                        @Override
                        public void bgTaskCancelled() {
                        }
                    };
            BgTask.start(
                    "Running tests",
                    new CancellableCallable<RunResult>() {

                        ListenableFuture<RunResult> result;

                        @Override
                        public RunResult call() throws Exception {
                            result
                            = TmcCoreSingleton.getInstance()
                            .test(projectInfo.getProjectDirAsPath());
                            return result.get();
                        }

                        @Override
                        public boolean cancel() {
                            return result.cancel(true);
                        }
                    },
                    bgTaskListener);
        }
    }

    private List<TestCaseResult> testResultsToTestCaseResults(
            ImmutableList<TestResult> testresults) {
        List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();
        for (TestResult result : testresults) {
            TestCaseResult testCase
                    = new TestCaseResult(result.name, result.passed, result.errorMessage);
            testCaseResults.add(testCase);
        }
        return testCaseResults;
    }
}
