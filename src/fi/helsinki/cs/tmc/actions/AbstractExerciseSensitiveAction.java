package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.NodeAction;

public abstract class AbstractExerciseSensitiveAction extends NodeAction {
    
    protected abstract ProjectMediator getProjectMediator();
    protected abstract CourseDb getCourseDb();
    
    @Override
    protected boolean enable(Node[] nodes) {
        return enable(projectsFromNodes(nodes).toArray(new Project[0]));
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    /*package (for tests)*/ boolean enable(Project ... projects) {
        if (projects.length == 0) {
            return false;
        }
        
        for (Project project : projects) {
            Exercise exercise = exerciseForProject(project);
            if (exercise != null && enabledFor(exercise)) {
                return true;
            }
        }
        return false;
    }
    
    protected boolean enabledFor(Exercise exercise) {
        return (exercise.isReturnable() && !exercise.hasDeadlinePassed());
    }
    
    protected List<Project> projectsFromNodes(Node[] nodes) {
        ArrayList<Project> result = new ArrayList<Project>();
        for (Node node : nodes) {
            Lookup lkp = node.getLookup();
            result.addAll(lkp.lookupAll(Project.class));
            result.addAll(projectsFromDataObjects(lkp.lookupAll(DataObject.class)));
        }
        return result;
    }
    
    private List<Project> projectsFromDataObjects(Collection<? extends DataObject> dataObjects) {
        ArrayList<Project> result = new ArrayList<Project>();
        for (DataObject dataObj : dataObjects) {
            Project project = projectFromDataObject(dataObj);
            if (project != null) {
                result.add(project);
            }
        }
        return result;
    }
    
    private Project projectFromDataObject(DataObject dataObj) {
        FileObject fileObj = dataObj.getPrimaryFile();
        return FileOwnerQuery.getOwner(fileObj);
    }
    
    protected Exercise exerciseForProject(Project project) {
        return getProjectMediator().tryGetExerciseForProject(getProjectMediator().wrapProject(project), getCourseDb());
    }
}
