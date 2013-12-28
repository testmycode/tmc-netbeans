package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.testHandler.testResultsHandler.TestRunHandler;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractExerciseSensitiveAction {

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TestRunHandler testRunHandler;
    
    public RunTestsLocallyAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.testRunHandler = new TestRunHandler();
        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    protected void performAction(Node[] nodes) {
        this.testRunHandler.performAction(projectsFromNodes(nodes).toArray(new Project[0]));
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
}
