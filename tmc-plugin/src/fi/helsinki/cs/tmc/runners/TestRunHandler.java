package fi.helsinki.cs.tmc.runners;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_MAVEN;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_SIMPLE;
import static fi.helsinki.cs.tmc.model.TmcProjectType.MAKEFILE;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.util.Arrays;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;

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
            final ExerciseRunner runner = getRunner(projectInfo);
            BgTask.start("Running tests", runner.getTestRunningTask(projectInfo), new BgTaskListener<TestRunResult>() {
                @Override
                public void bgTaskReady(TestRunResult result) {
                    if (!result.getCompilationSuccess()) {
                        dialogDisplayer.displayError("The code did not compile.");
                        return;
                    }
                    Exercise ex = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
                    boolean canSubmit = ex.isReturnable();
                    resultDisplayer.showLocalRunResult(result.getTestCaseResults(), canSubmit, new Runnable() {
                        @Override
                        public void run() {
                            exerciseSubmitter.performAction(projectInfo.getProject());
                        }
                    }, resultCollector);
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    log.log(INFO, "performAction of TestRunHandler failed with message: {0}, \ntrace: {1}",
                            new Object[]{ex.getMessage(), Throwables.getStackTraceAsString(ex)});
                    dialogDisplayer.displayError("Failed to run the tests: " + ex.getMessage());
                }

                @Override
                public void bgTaskCancelled() {
                }
            });
        }
    }

    private AbstractExerciseRunner getRunner(TmcProjectInfo projectInfo) {
        switch (projectInfo.getProjectType()) {
            case JAVA_MAVEN:
                return new MavenExerciseRunner();
            case JAVA_SIMPLE:
                return new AntExerciseRunner();
            case MAKEFILE:
                return new MakefileExerciseRunner();
            default:
                throw new IllegalArgumentException("Unknown project type: " + projectInfo.getProjectType());
        }
    }
}