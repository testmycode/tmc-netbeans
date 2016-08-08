package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;
import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractExerciseSensitiveAction implements Runnable {

    protected static final Logger log = Logger.getLogger(RunTestsLocallyAction.class.getName());

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private Project project;

    public RunTestsLocallyAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();

        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    protected void performAction(final Node[] nodes) {

        if (nodes.length == 1) {
            this.project = projectsFromNodes(nodes).get(0);

            WindowManager.getDefault().invokeWhenUIReady(this);
        }
    }

    @Override
    protected CourseDb getCourseDb() {
        return courseDb;
    }

    @Override
    protected ProjectMediator getProjectMediator() {
        return projectMediator;
    }

    @Override
    public String getName() {
        return "Run &tests locally";
    }

    @Override
    protected String iconResource() {
        // The setting in layer.xml doesn't work with NodeAction
        return "org/netbeans/modules/project/ui/resources/testProject.png";
    }

    @Override
    protected boolean enabledFor(Exercise exercise) {
        // Overridden to not care about the deadline
        return exercise.isRunTestsLocallyActionEnabled() && exercise.isReturnable();
    }

    @Override
    public void run() {
        Exercise exercise = exerciseForProject(project);
        
        projectMediator.saveAllFiles();
        final ResultCollector resultCollector = new ResultCollector(exercise);

        if (exercise != null) {
            ProgressObserver observer = new BridgingProgressObserver();
            Callable<RunResult> runTestsTask = TmcCore.get().runTests(observer, exercise);
            BgTask.start("Running tests for " + exercise.getName(), runTestsTask, observer, new BgTaskListener<RunResult>() {
                @Override
                public void bgTaskReady(RunResult result) {
                    log.log(Level.INFO, "Got test results: {0}", result);
                    resultCollector.setLocalTestResults(result);
                }

                @Override
                public void bgTaskCancelled() {
                    log.log(Level.INFO, "Run tests cancelled");
                    // NOP
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    log.log(Level.WARNING, "Test run failed:", ex);
                }
            });
        }

        ProgressObserver observer = new BridgingProgressObserver();
        Callable<ValidationResult> runCodeStyleValidationsTask = TmcCore.get().runCheckStyle(observer, exercise);
        BgTask.start("Running code style validations", runCodeStyleValidationsTask, observer, new BgTaskListener<ValidationResult>() {
            @Override
            public void bgTaskReady(ValidationResult result) {
                log.log(Level.INFO, "Got code style results: {0}", result);
                resultCollector.setValidationResult(result);
            }

            @Override
            public void bgTaskCancelled() {
                log.log(Level.INFO, "Run code style cancelled");
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                log.log(Level.WARNING, "Code style run failed:", ex);
            }
        });
    }
}
