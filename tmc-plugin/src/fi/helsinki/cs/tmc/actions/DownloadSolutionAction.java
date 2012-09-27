package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper.OverwritingDecider;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.DownloadSolutionAction")
@ActionRegistration(displayName = "#CTL_DownloadSolutionAction", lazy = true)
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -45)
})
@Messages("CTL_DownloadSolutionAction=Download suggested &solution")
public class DownloadSolutionAction extends AbstractExerciseSensitiveAction {
    private static final Logger logger = Logger.getLogger(DownloadSolutionAction.class.getName());
    private ProjectMediator projectMediator;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;

    public DownloadSolutionAction() {
        this.projectMediator = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }
    
    @Override
    public String getName() {
        return "Download suggested &solution";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enabledFor(Exercise exercise) {
        return exercise.getSolutionDownloadUrl() != null;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return new ActionMenuItem();
    }
    
    private JMenuItem getOriginalMenuPresenter() {
        return super.getMenuPresenter();
    }

    @Override
    protected ProjectMediator getProjectMediator() {
        return projectMediator;
    }

    @Override
    protected CourseDb getCourseDb() {
        return courseDb;
    }

    @Override
    protected void performAction(Node[] nodes) {
        projectMediator.saveAllFiles();
        
        for (Project project : projectsFromNodes(nodes)) {
            Exercise ex = exerciseForProject(project);
            if (ex.getSolutionDownloadUrl() == null) {
                // We shouldn't be visible any more.
                // See https://github.com/testmycode/tmc-netbeans/issues/45
                this.setEnabled(false);
                return;
            }
            
            if (dialogs.askYesNo("Are you sure you want to OVERWRITE your copy of\n" + ex.getName() + " with the suggested solution?", "Replace with solution?")) {
                downloadSolution(ex, projectMediator.wrapProject(project));
            }
        }
    }

    private void downloadSolution(final Exercise ex, final TmcProjectInfo proj) {
        ServerAccess serverAccess = new ServerAccess(TmcSettings.getDefault());
        CancellableCallable<byte[]> downloadTask = serverAccess.getDownloadingExerciseSolutionZipTask(ex);
        BgTask.start("Downloading solution for " + ex.getName(), downloadTask, new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(byte[] result) {
                unzipSolution(ex, proj, result);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.INFO, "Failed to download solution.", ex);
                dialogs.displayError("Failed to download solution.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        });
    }
    
    private void unzipSolution(final Exercise ex, final TmcProjectInfo proj, final byte[] data) {
        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                NbProjectUnzipper unzipper = new NbProjectUnzipper(solutionOverwriting);
                unzipper.unzipProject(data, proj.getProjectDirAsFile());
                return null;
            }
        };
        
        BgTask.start("Extracting solution", task, new BgTaskListener<Object>() {
            @Override
            public void bgTaskReady(Object result) {
                projectMediator.scanForExternalChanges(proj);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.INFO, "Failed to extract solution.", ex);
                dialogs.displayError("Failed to extract solution.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        });
    }
    
    private OverwritingDecider solutionOverwriting = new OverwritingDecider() {
        @Override
        public boolean mayOverwrite(String relPath) {
            return true;
        }
        @Override
        public boolean mayDelete(String relPath) {
            return false;
        }
    };
    
    private class ActionMenuItem extends JMenuItem implements DynamicMenuContent {
        public ActionMenuItem() {
            super(DownloadSolutionAction.this);
        }
        
        @Override
        public JComponent[] getMenuPresenters() {
            if (DownloadSolutionAction.this.isEnabled()) {
                return new JComponent[] { getOriginalMenuPresenter() };
            } else {
                return new JComponent[0];
            }
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] jcs) {
            return getMenuPresenters();
        }
        
    }
}
