package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.NodeAction;

// The action annotations don't work properly with NodeAction (NB 7.0)
// so this action is declared manually in layer.xml.
@Messages("CTL_SubmitExerciseAction=Su&bmit")
public final class SubmitExerciseAction extends NodeAction {

    private ServerAccess serverAccess;
    private LocalCourseCache courseCache;
    private ProjectMediator projectMediator;
    private SubmissionResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private ExerciseIconAnnotator iconAnnotator;

    public SubmitExerciseAction() {
        this(ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                SubmissionResultDisplayer.getInstance(),
                ConvenientDialogDisplayer.getDefault(),
                Lookup.getDefault().lookup(ExerciseIconAnnotator.class));
    }
    
    /*package*/ SubmitExerciseAction(
            ServerAccess serverAccess,
            LocalCourseCache courseCache,
            ProjectMediator projectMediator,
            SubmissionResultDisplayer resultDisplayer,
            ConvenientDialogDisplayer dialogDisplayer,
            ExerciseIconAnnotator iconAnnotator) {
        this.serverAccess = serverAccess;
        this.courseCache = courseCache;
        this.projectMediator = projectMediator;
        this.resultDisplayer = resultDisplayer;
        this.dialogDisplayer = dialogDisplayer;
        this.iconAnnotator = iconAnnotator;
        
        putValue("noIconInMenu", Boolean.TRUE);
    }
    
    @Override
    protected void performAction(Node[] nodes) {
        performAction(projectsFromNodes(nodes).toArray(new Project[0]));
    }
    
    /*package (for tests)*/ void performAction(Project ... projects) {
        for (Project nbProject : projects) {
            TmcProjectInfo tmcProject = projectMediator.wrapProject(nbProject);
            submitProject(tmcProject);
        }
    }
    
    private void submitProject(TmcProjectInfo project) {
        final Exercise exercise = projectMediator.tryGetExerciseForProject(project, courseCache);
        if (exercise == null) {
            return;
        }
        
        projectMediator.saveAllFiles();

        serverAccess.startSubmittingExercise(exercise, new BgTaskListener<SubmissionResult>() {
            @Override
            public void bgTaskReady(SubmissionResult result) {
                resultDisplayer.showResult(result);
                exercise.setAttempted(true);
                if (result.getStatus() == SubmissionResult.Status.OK) {
                    exercise.setCompleted(true);
                }
                iconAnnotator.updateAllIcons();
                courseCache.save();
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogDisplayer.displayError("Error submitting exercise.", ex);
            }
        });
    }

    @Override
    protected boolean enable(Node[] nodes) {
        return enable(projectsFromNodes(nodes).toArray(new Project[0]));
    }
    
    /*package (for tests)*/ boolean enable(Project ... projects) {
        if (projects.length == 0) {
            return false;
        }
        
        for (Project p : projects) {
            Exercise exercise = projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(p), courseCache);
            if (exercise != null) {
                return true;
            }
        }
        return false;
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

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    private static Set<Project> projectsFromNodes(Node[] nodes) {
        HashSet<Project> result = new HashSet<Project>();
        for (Node node : nodes) {
            Lookup lkp = node.getLookup();
            result.addAll(lkp.lookupAll(Project.class));
            result.addAll(projectsFromDataObjects(lkp.lookupAll(DataObject.class)));
        }
        return result;
    }
    
    private static List<Project> projectsFromDataObjects(Collection<? extends DataObject> dataObjects) {
        ArrayList<Project> result = new ArrayList<Project>();
        for (DataObject dataObj : dataObjects) {
            result.add(projectFromDataObject(dataObj));
        }
        return result;
    }
    
    private static Project projectFromDataObject(DataObject dataObj) {
        FileObject fileObj = dataObj.getPrimaryFile();
        return FileOwnerQuery.getOwner(fileObj);
    }
}
