package fi.helsinki.cs.tmc.exerciseSubmitter;

import fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates;
import fi.helsinki.cs.tmc.actions.ServerErrorHelper;
import fi.helsinki.cs.tmc.actions.SubmitExerciseAction;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.SubmissionResultWaiter;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.SubmissionResultWaitingDialog;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.project.Project;

public class ExerciseSubmitter {

    private static final Logger log = Logger.getLogger(SubmitExerciseAction.class.getName());

    public static class InvokedEvent implements TmcEvent {
        public final TmcProjectInfo projectInfo;
        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    private NBTmcSettings settings;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;

    public ExerciseSubmitter() {
        this.settings = NBTmcSettings.getDefault();
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }


    public void performAction(Project ... projects) {
        for (Project nbProject : projects) {
            TmcProjectInfo tmcProject = projectMediator.wrapProject(nbProject);
            eventBus.post(new InvokedEvent(tmcProject));
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

        final SubmissionResultWaitingDialog dialog = SubmissionResultWaitingDialog.createAndShow();

        final BgTaskListener<ServerAccess.SubmissionResponse> submissionListener = new BgTaskListener<ServerAccess.SubmissionResponse>() {
            @Override
            public void bgTaskReady(ServerAccess.SubmissionResponse response) {
                final SubmissionResultWaiter waitingTask = new SubmissionResultWaiter(response.submissionUrl.toString(), dialog);
                dialog.setTask(waitingTask);

                BgTask.start("Waiting for results from server.", waitingTask, new BgTaskListener<SubmissionResult>() {

                    @Override
                    public void bgTaskReady(SubmissionResult result) {

                        dialog.close();

                        final ResultCollector resultCollector = new ResultCollector(exercise);
                        resultCollector.setValidationResult(result.getValidationResult());
                        resultDisplayer.showSubmissionResult(exercise, result, resultCollector);

                        // We change exercise state as a first approximation,
                        // then refresh from the server and potentially notify the user
                        // as we might have unlocked new exercises.
                        exercise.setAttempted(true);

                        if (result.getStatus() == SubmissionResult.Status.OK) {
                            exercise.setCompleted(true);
                        }

                        courseDb.save();

                        new CheckForNewExercisesOrUpdates(true, false).run();
                    }

                    @Override
                    public void bgTaskCancelled() {
                        dialog.close();
                    }

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        log.log(Level.INFO, "Error waiting for results from server.", ex);
                        String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                        dialogDisplayer.displayError("Error trying to get test results.", ex);
                        dialog.close();
                    }
                });
            }

            @Override
            public void bgTaskCancelled() {
                dialog.close();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                log.log(Level.INFO, "Error submitting exercise.", ex);
                String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                dialogDisplayer.displayError("Error submitting exercise.", ex);
                dialog.close();
            }
        };

        final String errorMsgLocale = settings.getErrorMsgLocale().toString();

        BgTask.start("Zipping up " + exercise.getName(), new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                RecursiveZipper zipper = new RecursiveZipper(project.getProjectDirAsFile(), project.getZippingDecider());
                return zipper.zipProjectSources();
            }
        }, new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(byte[] zipData) {
                Map<String, String> extraParams = new HashMap<String, String>();
                extraParams.put("error_msg_locale", errorMsgLocale);

                CancellableCallable<ServerAccess.SubmissionResponse> submitTask = serverAccess.getSubmittingExerciseTask(exercise, zipData, extraParams);
                dialog.setTask(submitTask);
                BgTask.start("Sending " + exercise.getName(), submitTask, submissionListener);
            }

            @Override
            public void bgTaskCancelled() {
                submissionListener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                submissionListener.bgTaskFailed(ex);
            }
        });
    }
}
