package fi.helsinki.cs.tmc.runners;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.langs.RunResult;
import static fi.helsinki.cs.tmc.langs.RunResult.Status.COMPILE_FAILED;
import fi.helsinki.cs.tmc.langs.TestResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_MAVEN;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_SIMPLE;
import static fi.helsinki.cs.tmc.model.TmcProjectType.MAKEFILE;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import hy.tmc.core.exceptions.TmcCoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

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
            final ProgressHandle runningTestsLocally = ProgressHandleFactory.createSystemHandle(
                    "Running tests.");
            runningTestsLocally.start();
            try {
                ListenableFuture<RunResult> result = TmcCoreSingleton.getInstance().test(projectInfo.getProjectDirAbsPath(), NBTmcSettings.getDefault());
                Futures.addCallback(result, new FutureCallback<RunResult>() {
                    @Override
                    public void onSuccess(final RunResult result) {
                        runningTestsLocally.finish();
                        explainResults(result, projectInfo, resultCollector);
                    }

                    @Override
                    public void onFailure(final Throwable ex) {
                        runningTestsLocally.finish();
                        explainFailure(ex);
                    }

                });
            } catch (TmcCoreException ex) {
                runningTestsLocally.finish();
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void explainFailure(final Throwable ex) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.log(INFO, "performAction of TestRunHandler failed with message: {0}, \ntrace: {1}",
                        new Object[]{ex.getMessage(), Throwables.getStackTraceAsString(ex)});
                dialogDisplayer.displayError("Failed to run the tests: " + ex.getMessage());
            }
        });
    }

    private void explainResults(final RunResult result, final TmcProjectInfo projectInfo, final ResultCollector resultCollector) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (result.status == COMPILE_FAILED) {
                    dialogDisplayer.displayError("The code did not compile.");
                    return;
                }
                Exercise ex = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
                boolean canSubmit = ex.isReturnable();
                List<TestCaseResult> list = testResultsToTestCaseResults(result.testResults);
                resultDisplayer.showLocalRunResult(list, canSubmit, new Runnable() {
                    @Override
                    public void run() {
                        exerciseSubmitter.performAction(projectInfo.getProject());
                    }
                }, resultCollector);
            }
        });
    }

    private List<TestCaseResult> testResultsToTestCaseResults(ImmutableList<TestResult> testresults) {
        List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();
        for (TestResult result : testresults) {
            TestCaseResult testCase = new TestCaseResult(result.name, result.passed, result.errorMessage);
            testCaseResults.add(testCase);
        }
        return testCaseResults;
    }
}
