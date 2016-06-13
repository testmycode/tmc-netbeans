package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.PastebinDialog;
import fi.helsinki.cs.tmc.ui.PastebinResponseDialog;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

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
import java.util.concurrent.Callable;
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
public final class PastebinAction extends AbstractExerciseSensitiveAction {

    private static final Logger log = Logger.getLogger(RequestReviewAction.class.getName());
    private TmcCoreSettingsImpl settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;

    public PastebinAction() {
        this.settings = ((TmcCoreSettingsImpl) TmcSettingsHolder.get());
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
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
                eventBus.post(new PastebinAction.InvokedEvent(projectInfo));
                showPasteRequestDialog(exercise);
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

    private void showPasteRequestDialog(final Exercise exercise) {
        final PastebinDialog dialog = new PastebinDialog(exercise);
        dialog.setOkListener(
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = dialog.getMessageForReviewer().trim();
                submitPaste(exercise, message);
            }
        });
        dialog.setVisible(true);
    }

    private void submitPaste(
            final Exercise exercise,
            final String messageForReviewer) {
        projectMediator.saveAllFiles();

        Callable<URI> pasteTask = TmcCore.get()
                .pasteWithComment(ProgressObserver.NULL_OBSERVER, exercise, messageForReviewer);

        BgTask.start(
                "Sending " + exercise.getName(), pasteTask, new BgTaskListener<URI>() {
            @Override
            public void bgTaskReady(URI result) {
                new PastebinResponseDialog(result.toString())
                        .setVisible(true);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError(
                        "Failed to send exercise to pastebin", ex);
            }
        });
    }

    @Override
    public String getName() {
        return "Send code to TMC Pastebin";
    }

    public static class InvokedEvent implements TmcEvent {

        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }
}
