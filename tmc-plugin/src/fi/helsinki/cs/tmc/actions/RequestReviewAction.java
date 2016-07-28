package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory;
import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory.SubmissionResponse;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;

import com.google.gson.Gson;

import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.ui.CodeReviewRequestDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.nio.charset.Charset;
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
import org.openide.util.NbBundle;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.RequestReviewAction")
@ActionRegistration(displayName = "#CTL_RequestReviewAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -5, separatorAfter = 0),
    @ActionReference(path = "Projects/Actions", position = 1350, separatorBefore = 1330,
            separatorAfter = 1360) // Positioning y u no work?
})
@NbBundle.Messages("CTL_RequestReviewAction=Request code review")
public class RequestReviewAction extends AbstractExerciseSensitiveAction {

    private static final Logger log = Logger.getLogger(RequestReviewAction.class.getName());
    private TmcCoreSettingsImpl settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;

    public RequestReviewAction() {
        this.settings = (TmcCoreSettingsImpl)TmcSettingsHolder.get();
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
    protected boolean enabledFor(Exercise exercise) {
        return exercise.isCodeReviewRequestsEnabled() && super.enabledFor(exercise);
    }

    @Override
    protected void performAction(Node[] nodes) {
        List<Project> project = projectsFromNodes(nodes);
        if (project.size() == 1) {
            TmcProjectInfo projectInfo = projectMediator.wrapProject(project.get(0));
            Exercise exercise = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);
            if (exercise != null) {
                showReviewRequestDialog(projectInfo, exercise);
            } else {
                log.log(Level.WARNING, "RequestReviewAction called in a context without a valid TMC project.");
            }
        } else {
            log.log(Level.WARNING, "RequestReviewAction called in a context with {0} projects", project.size());
        }
    }

    private void showReviewRequestDialog(final TmcProjectInfo projectInfo, final Exercise exercise) {
        final CodeReviewRequestDialog dialog = new CodeReviewRequestDialog(exercise);
        dialog.setOkListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = dialog.getMessageForReviewer().trim();
                requestCodeReviewFor(projectInfo, exercise, message);
            }
        });
        dialog.setVisible(true);
    }

    private void requestCodeReviewFor(final TmcProjectInfo projectInfo, final Exercise exercise,
            final String messageForReviewer) {
        projectMediator.saveAllFiles();

        final String errorMsgLocale = settings.getErrorMsgLocale().toString();

        // TODO: use core
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

                extraParams.put("request_review", "1");
                if (!messageForReviewer.isEmpty()) {
                    extraParams.put("message_for_reviewer", messageForReviewer);
                }

                
                Callable<SubmissionResponse> submitTask = 
                        new TmcServerCommunicationTaskFactory().getSubmittingExerciseTask(exercise, zipData, extraParams);

                BgTask.start("Sending " + exercise.getName(), submitTask, new BgTaskListener<SubmissionResponse>() {
                    @Override
                    public void bgTaskReady(SubmissionResponse result) {
                        sendLoggableEvent(exercise, result.submissionUrl);

                        dialogs.displayMessage("Code submitted for review.\n"
                                + "You will be notified when an instructor has reviewed your code.");
                    }

                    @Override
                    public void bgTaskCancelled() {
                    }

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        dialogs.displayError("Failed to submit exercise for code review", ex);
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
        return "Request code review";
    }

    private void sendLoggableEvent(Exercise exercise, URI submissionUrl) {
        String courseName = courseDb.getCurrentCourseName();
        if (courseName != null) {
            ReviewRequested dataObject = new ReviewRequested(exercise, submissionUrl);
            String json = new Gson().toJson(dataObject);
            byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));

            LoggableEvent event = new LoggableEvent(exercise.getCourseName(), exercise.getName(), "review_requested", jsonBytes);
            eventBus.post(event);
        }
    }

    private static class ReviewRequested {

        public final String courseName;
        public final String exerciseName;
        public final String submissionUrl;

        private ReviewRequested(Exercise exercise, URI submissionUrl) {
            this.courseName = exercise.getCourseName();
            this.exerciseName = exercise.getName();
            this.submissionUrl = submissionUrl.toString();
        }
    }
}
