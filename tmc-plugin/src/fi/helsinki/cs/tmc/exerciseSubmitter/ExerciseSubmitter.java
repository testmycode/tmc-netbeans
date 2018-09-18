package fi.helsinki.cs.tmc.exerciseSubmitter;

import fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.SubmissionResultWaitingDialog;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

import org.netbeans.api.project.Project;

public class ExerciseSubmitter {

    private static final Logger log = Logger.getLogger(ExerciseSubmitter.class.getName());

    public static class InvokedEvent implements TmcEvent {

        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;

    public ExerciseSubmitter() {
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

        Callable<SubmissionResult> callable = TmcCore.get().submit(ProgressObserver.NULL_OBSERVER, exercise);

        final SubmissionResultWaitingDialog dialog = SubmissionResultWaitingDialog.createAndShow();

        BgTask.start("Waiting for results from server.", callable, new BgTaskListener<SubmissionResult>() {
            @Override
            public void bgTaskReady(SubmissionResult result) {

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

                dialog.close();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        courseDb.save();
                        new CheckForNewExercisesOrUpdates(true, false).run();
                        return null;
                    }
                }.run();
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
}
