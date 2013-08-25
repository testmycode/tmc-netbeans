package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.CodeReviewRequestDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.PastebinDialog;
import fi.helsinki.cs.tmc.ui.PastebinResponseDialog;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;
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
import org.openide.cookies.EditorCookie;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
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
    private TmcSettings settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    public PastebinAction() {
        this.settings = TmcSettings.getDefault();
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
    boolean enable(Project... projects) {
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

        BgTask.start("Zipping up " + exercise.getName(), new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                RecursiveZipper zipper = new RecursiveZipper(projectInfo.getProjectDirAsFile(), projectInfo.getZippingDecider());
                return zipper.zipProjectSources();
            }
        }, new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(byte[] zipData) {
                Map<String, String> extraParams = new HashMap<String, String>();
                extraParams.put("error_msg_locale", errorMsgLocale);
                extraParams.put("paste", "1");
                if (!messageForReviewer.isEmpty()) {
                    extraParams.put("message_for_paste", messageForReviewer);
                }

                final ServerAccess sa = new ServerAccess();
                CancellableCallable<URI> submitTask = sa
                        .getSubmittingExerciseTask(exercise, zipData, extraParams);

                BgTask.start("Sending " + exercise.getName(), submitTask, new BgTaskListener<URI>() {
                    @Override
                    public void bgTaskReady(URI result) {
                        new PastebinResponseDialog(sa.getRespJson().get("paste_url")
                                .getAsString()).setVisible(true);

                    }

                    @Override
                    public void bgTaskCancelled() {
                    }

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        dialogs.displayError("Failed to send exercise to pastebin", ex);
                    }
                });
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError("Failed to zip up exercise", ex);
            }
        });
    }

    @Override
    public String getName() {
        return "Send code to TMC Pastebin";
    }
}