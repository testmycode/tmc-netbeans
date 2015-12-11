package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.PastebinDialog;
import fi.helsinki.cs.tmc.ui.PastebinResponseDialog;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.util.concurrent.ListenableFuture;

import org.netbeans.api.project.Project;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.PastebinAction")
@ActionRegistration(displayName = "#CTL_PastebinAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -17),
    @ActionReference(
        path = "Projects/Actions",
        position = 1340,
        separatorBefore = 1330,
        separatorAfter = 1360
    )
})
@Messages("CTL_PastebinAction=Send code to Pastebin")
//TODO: This is a horribly copypasted, then mangled version of RequestReviewAction
//plz remove everything that isn't needed here. --kviiri
public final class PastebinAction extends AbstractExerciseSensitiveAction {

    private static final Logger log = Logger.getLogger(RequestReviewAction.class.getName());
    private NbTmcSettings settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private final ConvenientDialogDisplayer dialogs;

    public PastebinAction() {
        this.settings = NbTmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
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
    public boolean enable(Project... projects) {
        if (projects.length > 1) {
            return false; // One at a time please
        } else {
            return super.enable(projects);
        }
    }

    @Override
    protected void performAction(Node[] nodes) {
        List<Project> project = projectsFromNodes(nodes);
        if (project.size() == 1) {
            TmcProjectInfo projectInfo = projectMediator.wrapProject(project.get(0));
            Exercise exercise = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
            if (exercise != null) {
                showPasteRequestDialog(projectInfo, exercise);
            } else {
                log.log(
                        Level.WARNING,
                        "PastebinAction called in a context without a valid TMC project.");
            }
        } else {
            log.log(
                    Level.WARNING,
                    "PastebinAction called in a context with {0} projects",
                    project.size());
        }
    }

    private void showPasteRequestDialog(final TmcProjectInfo projectInfo, final Exercise exercise) {
        final PastebinDialog dialog = new PastebinDialog(exercise);
        dialog.setOkListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String message = dialog.getMessageForReviewer().trim();
                        submitPaste(projectInfo, exercise, message);
                    }
                });
        dialog.setVisible(true);
    }

    private void submitPaste(
            final TmcProjectInfo projectInfo,
            final Exercise exercise,
            final String messageForReviewer) {
        projectMediator.saveAllFiles();
        BgTask.start(
                "Sending tmc-paste",
                new CancellableCallable<URI>() {
                    ListenableFuture<URI> result;

                    @Override
                    public URI call() throws Exception {
                        log.log(Level.INFO, "Pre submit");
                        result =
                                TmcCoreSingleton.getInstance()
                                        .pasteWithComment(
                                                projectInfo.getProjectDirAsPath(),
                                                messageForReviewer);
                        return result.get();
                    }

                    @Override
                    public boolean cancel() {
                        return result.cancel(true);
                    }
                },
                new PasteResult());
    }

    class PasteResult implements BgTaskListener<URI> {

        @Override
        public void bgTaskReady(URI uri) {
            new PastebinResponseDialog(uri.toString()).setVisible(true);
        }

        @Override
        public void bgTaskCancelled() {}

        @Override
        public void bgTaskFailed(Throwable ex) {
            dialogs.displayError("Failed to send exercise to pastebin. \n" 
                    + ServerErrorHelper.getServerExceptionMsg(ex));
        }
    }

    @Override
    public String getName() {
        return "Send code to TMC Pastebin";
    }
}
