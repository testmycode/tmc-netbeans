package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;

import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
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
        if (exercise != null) {
            try {
                ResultCollector resultCollector = new ResultCollector(exercise);
                resultCollector.setLocalTestResults(TmcCore.get().runTests(ProgressObserver.NULL_OBSERVER, exercise).call());
                resultCollector.setValidationResult(TmcCore.get().runCheckStyle(ProgressObserver.NULL_OBSERVER, exercise).call());
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
