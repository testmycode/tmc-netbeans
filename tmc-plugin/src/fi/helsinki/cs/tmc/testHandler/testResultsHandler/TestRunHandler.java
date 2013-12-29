package fi.helsinki.cs.tmc.testHandler.testResultsHandler;

import fi.helsinki.cs.tmc.testHandler.TestRunner.MavenExerciseTestRunner;
import fi.helsinki.cs.tmc.testHandler.TestRunner.AbstractExerciseTestRunner;
import fi.helsinki.cs.tmc.testHandler.TestRunner.AntExerciseTestRunner;
import fi.helsinki.cs.tmc.testHandler.TestRunner.MakeFileExerciseTestRunner;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_MAVEN;
import static fi.helsinki.cs.tmc.model.TmcProjectType.JAVA_SIMPLE;
import static fi.helsinki.cs.tmc.model.TmcProjectType.MAKEFILE;
import fi.helsinki.cs.tmc.model.TmcSettings;
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
    private TmcSettings settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;
    private AbstractExerciseTestRunner arrt = null;

    public TestRunHandler() {
        this.settings = TmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }

    public void performAction(Project... projects) {
        projectMediator.saveAllFiles();
        for (final Project project : projects) {
            final TmcProjectInfo projectInfo = projectMediator.wrapProject(project);
            eventBus.post(new InvokedEvent(projectInfo));
            arrt = getRunner(projectInfo);
            BgTask.start("Compiling project", arrt.startCompilingProject(projectInfo), new BgTaskListener<Integer>() {
                @Override
                public void bgTaskReady(Integer result) {
                    if (result == 0) {
                        arrt.startRunningTests(projectInfo);
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

    private AbstractExerciseTestRunner getRunner(TmcProjectInfo projectInfo) {
        switch (projectInfo.getProjectType()) {
            case JAVA_MAVEN:
                return new MavenExerciseTestRunner();
            case JAVA_SIMPLE:
                return new AntExerciseTestRunner();
            case MAKEFILE:
                return new MakeFileExerciseTestRunner();
            default:
                throw new IllegalArgumentException("Unknown project type: " + projectInfo.getProjectType());
        }
    }
}
