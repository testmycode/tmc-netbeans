package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.runners.CheckstyleRunHandler;
import fi.helsinki.cs.tmc.runners.TestRunHandler;
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
    private CheckstyleRunHandler checkstyleRunHandler;
    private TestRunHandler testRunHandler;
    private Project project;

    public RunTestsLocallyAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.checkstyleRunHandler = new CheckstyleRunHandler();
        this.testRunHandler = new TestRunHandler();
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
        return exercise.isReturnable();
    }

    @Override
    public void run() {
        Exercise exercise = exerciseForProject(project);
        if (exercise == null) {
            return;
        }

        ResultCollector resultCollector = new ResultCollector(exercise);

        if (exercise.isCheckstyleEnabled()) {
            this.checkstyleRunHandler.performAction(resultCollector, project);
        }

        this.testRunHandler.performAction(resultCollector, project);
    }
}
