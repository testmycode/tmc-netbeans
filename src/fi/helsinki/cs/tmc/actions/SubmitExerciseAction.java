package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.SubmissionResultWaiter;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.CancellableRunnable;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectZipper;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

// The action annotations don't work properly with NodeAction (NB 7.0)
// so this action is declared manually in layer.xml.
@Messages("CTL_SubmitExerciseAction=Su&bmit")
public final class SubmitExerciseAction extends AbstractTmcRunAction {

    private static final Logger log = Logger.getLogger(SubmitExerciseAction.class.getName());
    
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private NbProjectZipper zipper;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;

    public SubmitExerciseAction() {
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.zipper = NbProjectZipper.getDefault();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        
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
                                    resultDisplayer.showSubmissionResult(result);
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
                            dialogDisplayer.displayError("Error trying to get test results.", ex);
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

        BgTask.start("Zipping up " + exercise.getName(), new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                return zipper.zipProjectSources(FileUtil.toFile(project.getProjectDir()));
            }
        }, new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(byte[] zipData) {
                CancellableCallable<URI> task = serverAccess.getSubmittingExerciseTask(exercise, zipData);
                BgTask.start("Sending " + exercise.getName(), task, uriListener);
            }

            @Override
            public void bgTaskCancelled() {
                uriListener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                uriListener.bgTaskFailed(ex);
            }
        });
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
