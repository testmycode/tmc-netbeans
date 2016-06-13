package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.spyware.SpywareFacade;

import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

// The action annotations don't work properly with NodeAction (NB 7.0)
// so this action is declared manually in layer.xml.
@Messages("CTL_SubmitExerciseAction=Su&bmit")
public final class SubmitExerciseAction extends AbstractExerciseSensitiveAction {

    private CourseDb courseDb;
    private ProjectMediator projectMediator;

    public SubmitExerciseAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();

        putValue("noIconInMenu", Boolean.TRUE);
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
    protected void performAction(Node[] nodes) {
        new ExerciseSubmitter().performAction(projectsFromNodes(nodes).toArray(new Project[0]));
        SpywareFacade.sendNow();
    }

    @Override
    public String getName() {
        return "Su&bmit";
    }

    @Override
    protected String iconResource() {
        // The setting in layer.xml doesn't work with NodeAction
        return "fi/helsinki/cs/tmc/actions/submit.png";
    }
}
