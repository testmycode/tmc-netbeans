package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.SubmissionResultWaiter;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.CancellableRunnable;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectZipper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
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

    private static final Logger log = Logger.getLogger(SubmitExerciseAction.class.getName());
    
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private NbProjectZipper zipper;
    private ProjectMediator projectMediator;
    private SubmissionResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;

    public SubmitExerciseAction() {
        this.serverAccess = ServerAccess.create();
        this.courseDb = CourseDb.getInstance();
        this.zipper = NbProjectZipper.getDefault();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = SubmissionResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        
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
    
    private void submitProject(final TmcProjectInfo project) {
        final Exercise exercise = projectMediator.tryGetExerciseForProject(project, courseDb);
        if (exercise == null || !exercise.isReturnable()) {
            return;
        }
        
        projectMediator.saveAllFiles();
        
        // Oh what a mess :/
        
        final BgTaskListener<URI> uriListener = new BgTaskListener<URI>() {
            @Override
            public void bgTaskReady(URI submissionUri) {
                final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Waiting for results from server.");
                final SubmissionResultWaiter waiter = new SubmissionResultWaiter(submissionUri, progressHandle);
                
                ProgressUtils.showProgressDialogAndRun(new CancellableRunnable() {
                    @Override
                    public void run() {
                        try {
                            progressHandle.switchToIndeterminate();
                            final SubmissionResult result = waiter.call();
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    resultDisplayer.showResult(result);
                                    exercise.setAttempted(true);
                                    if (result.getStatus() == SubmissionResult.Status.OK) {
                                        exercise.setCompleted(true);
                                    }
                                    courseDb.save();
                                }
                            });
                            
                        } catch (InterruptedException ex) {
                        } catch (TimeoutException ex) {
                            log.log(Level.INFO, "Timeout waiting for results from server.", ex);
                            dialogDisplayer.displayError("This is taking too long.\nPlease try again in a few minutes or ask someone to look into the problem.");
                        } catch (Exception ex) {
                            log.log(Level.INFO, "Error waiting for results from server.", ex);
                        }
                    }

                    @Override
                    public boolean cancel() {
                        return waiter.cancel();
                    }
                }, progressHandle, true);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                log.log(Level.INFO, "Error submitting exercise.", ex);
                dialogDisplayer.displayError("Error submitting exercise.", ex);
            }
        };

        BgTask.start("Zipping up " + exercise.getName(), new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(byte[] zipData) {
                serverAccess.startSubmittingExercise(exercise, zipData, uriListener);
            }

            @Override
            public void bgTaskCancelled() {
                uriListener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                uriListener.bgTaskFailed(ex);
            }
        }, new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                return zipper.zipProjectSources(FileUtil.toFile(project.getProjectDir()));
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
            Exercise exercise = projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(p), courseDb);
            if (exercise != null && exercise.isReturnable()) {
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
