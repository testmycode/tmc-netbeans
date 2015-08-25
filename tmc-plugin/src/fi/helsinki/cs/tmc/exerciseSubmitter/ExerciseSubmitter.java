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
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.SubmissionResultWaitingDialog;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;

import org.netbeans.api.project.Project;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ExerciseSubmitter {

    private static final Logger log = Logger.getLogger(SubmitExerciseAction.class.getName());

    public static class InvokedEvent implements TmcEvent {

        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    private NbTmcSettings settings;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;

    public ExerciseSubmitter() {
        this.settings = NbTmcSettings.getDefault();
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }

    public void performAction(Project... projects) {
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

        CancellableCallable<SubmissionResult> submitAndPollTask = new CancellableCallable<SubmissionResult>() {

            ListenableFuture<SubmissionResult> lf;

            @Override
            public SubmissionResult call() throws Exception {
                lf = TmcCoreSingleton.getInstance().submit(project.getProjectDirAsPath());
                return lf.get();
            }

            @Override
            public boolean cancel() {
                return lf.cancel(true);
            }
        };

        dialog.setTask(submitAndPollTask);

        BgTask.start("Submitting and waiting for results from server.", submitAndPollTask, new BgTaskListener<SubmissionResult>() {

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
                if (!Strings.isNullOrEmpty(msg)) {
                    dialogDisplayer.displayError("Error trying to get test results.", ex);
                }
                dialog.close();
            }
        });
    }
}