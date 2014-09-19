package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.runners.CheckstyleRunHandler;
import fi.helsinki.cs.tmc.runners.TestRunHandler;
import java.awt.Component;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractExerciseSensitiveAction implements Runnable {

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private CheckstyleRunHandler checkstyleRunHandler;
    private TestRunHandler testRunHandler;
    private Project project;

    private static final String GREEN_EYE = "testProjectGreen24.png";
    private static final String RED_EYE = "testProjectRed24.png";

    public RunTestsLocallyAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.checkstyleRunHandler = new CheckstyleRunHandler();
        this.testRunHandler = new TestRunHandler();
        putValue("noIconInMenu", Boolean.TRUE);

        URL imgURL = getClass().getResource(GREEN_EYE);
        setIcon(new ImageIcon(imgURL, ""));
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
        return null;
    }

    private Icon getIcon(Exercise exercise) {
        String name = GREEN_EYE;
        if (exercise != null) {
            name = exercise.isReturnable() ? GREEN_EYE : RED_EYE;
        }
        System.out.println("exercise: " + exercise + " color: " + name);
        URL imgURL = getClass().getResource(name);
        return new ImageIcon(imgURL, "");
    }

    private void updateIcon(Exercise exercise) {
        setIcon(getIcon(exercise));
    }

    @Override
    protected boolean enabledFor(Exercise exercise) {
        // This gets always called when changing projects related to tmc
        updateIcon(exercise);
        return true;
    }

    @Override
    public void run() {

        Exercise exercise = exerciseForProject(project);
        if (exercise != null) {
            ResultCollector resultCollector = new ResultCollector(exercise);
            this.checkstyleRunHandler.performAction(resultCollector, project);
            this.testRunHandler.performAction(resultCollector, project);
        }
    }
}
