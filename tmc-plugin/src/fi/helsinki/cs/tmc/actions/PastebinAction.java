package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.PastebinDialog;
import fi.helsinki.cs.tmc.ui.PastebinResponseDialog;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;
import hy.tmc.core.exceptions.TmcCoreException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "TMC",
        id = "fi.helsinki.cs.tmc.actions.PastebinAction")
@ActionRegistration(
        displayName = "#CTL_PastebinAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -17),
    @ActionReference(path = "Projects/Actions", position = 1340, separatorBefore = 1330,
            separatorAfter = 1360)
})
@Messages("CTL_PastebinAction=Send code to Pastebin")
//TODO: This is a horribly copypasted, then mangled version of RequestReviewAction
//plz remove everything that isn't needed here. --kviiri
public final class PastebinAction extends AbstractExerciseSensitiveAction {

    private static final Logger log = Logger.getLogger(RequestReviewAction.class.getName());
    private NBTmcSettings settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    public PastebinAction() {
        this.settings = NBTmcSettings.getDefault();
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
                log.log(Level.WARNING, "PastebinAction called in a context without a valid TMC project.");
            }
        } else {
            log.log(Level.WARNING, "PastebinAction called in a context with {0} projects", project.size());
        }
    }

    private void showPasteRequestDialog(final TmcProjectInfo projectInfo, final Exercise exercise) {
        final PastebinDialog dialog = new PastebinDialog(exercise);
        dialog.setOkListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = dialog.getMessageForReviewer().trim();
                submitPaste(projectInfo, exercise, message);
            }
        });
        dialog.setVisible(true);
    }

    private void submitPaste(final TmcProjectInfo projectInfo, final Exercise exercise,
            final String messageForReviewer) {
        projectMediator.saveAllFiles();
        final String errorMsgLocale = settings.getErrorMsgLocale().toString();
        try {
            ListenableFuture<URI> result = TmcCoreSingleton.getInstance().pasteWithComment(projectInfo.getProjectDirAbsPath(), settings, messageForReviewer);
            Futures.addCallback(result, new PasteResult());
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    class PasteResult implements FutureCallback<URI> {

        @Override
        public void onSuccess(URI ur) {
            new PastebinResponseDialog(ur.toString()).setVisible(true);
        }

        @Override
        public void onFailure(Throwable thrwbl) {
            dialogs.displayError("Failed to send exercise to pastebin", thrwbl);
        }
    }

    @Override
    public String getName() {
        return "Send code to TMC Pastebin";
    }
}
