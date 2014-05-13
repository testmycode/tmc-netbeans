package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.data.Exercise;
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
import org.netbeans.api.project.Project;

public class TestRunHandler {

    public static class InvokedEvent implements TmcEvent {
        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;
    private TestResultDisplayer resultDisplayer;
    private ExerciseSubmitter exerciseSubmitter;
    private CourseDb courseDb;

    public TestRunHandler() {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.exerciseSubmitter = new ExerciseSubmitter();
        this.courseDb = CourseDb.getInstance();
    }

    public void performAction(Project... projects) {
        projectMediator.saveAllFiles();
        for (final Project project : projects) {
            final TmcProjectInfo projectInfo = projectMediator.wrapProject(project);
            eventBus.post(new InvokedEvent(projectInfo));
            final ExerciseRunner runner = getRunner(projectInfo);
            BgTask.start("Compiling project", runner.getCompilingTask(projectInfo), new BgTaskListener<Integer>() {
                @Override
                public void bgTaskReady(Integer result) {
                    if (result == 0) {
                        startRunningTests(runner, projectInfo);
                    } else {
                        dialogDisplayer.displayError("The code did not compile.");
                    }
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    dialogDisplayer.displayError("Failed to compile the code.");
                }

                @Override
                public void bgTaskCancelled() {
                }
            });
        }
    }

    private void startRunningTests(ExerciseRunner runner, final TmcProjectInfo projectInfo) {
        BgTask.start("Running tests", runner.getTestRunningTask(projectInfo), new BgTaskListener<TestRunResult>() {
            @Override
            public void bgTaskReady(TestRunResult result) {
                Exercise ex = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
                boolean canSubmit = ex.isReturnable();
                resultDisplayer.showLocalRunResult(result.getTestCaseResults(), canSubmit, new Runnable() {
                    @Override
                    public void run() {
                        exerciseSubmitter.performAction(projectInfo.getProject());
                    }
                });
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogDisplayer.displayError("Failed to compile the code.");
            }

            @Override
            public void bgTaskCancelled() {
            }
        });
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
