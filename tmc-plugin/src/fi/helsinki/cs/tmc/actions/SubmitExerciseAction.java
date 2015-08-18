package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.SubmissionResultWaitingDialog;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

// The action annotations don't work properly with NodeAction (NB 7.0)
// so this action is declared manually in layer.xml.
@Messages("CTL_SubmitExerciseAction=Su&bmit")
public final class SubmitExerciseAction extends AbstractExerciseSensitiveAction {

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private final TmcCore core;
    private NBTmcSettings settings;
    private ConvenientDialogDisplayer dialogs;

    public static class InvokedEvent implements TmcEvent {
        public final TmcProjectInfo projectInfo;
        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }

    public SubmitExerciseAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.core = TmcCoreSingleton.getInstance();
        this.settings = NBTmcSettings.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

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

        projectMediator.saveAllFiles();

        Project[] projects = projectsFromNodes(nodes).toArray(new Project[0]);
        for (Project project : projects) {
            TmcProjectInfo info = projectMediator.wrapProject(project);
            TmcEventBus.getDefault().post(new SubmitExerciseAction.InvokedEvent(info));
            submitProject(info);
        }
    }

    private void submitProject(TmcProjectInfo info) {

        Exercise exercise = projectMediator.tryGetExerciseForProject(info, courseDb);
        final SubmissionResultWaitingDialog dialog = SubmissionResultWaitingDialog.createAndShow();
        try {
            ListenableFuture<SubmissionResult> result;
            result = core.submit(info.getProjectDirAbsPath());
            Futures.addCallback(result, new SubmissionCallback(exercise, dialog));
        } catch (TmcCoreException ex) {
            String message = "There was an error submitting project " + info.getProjectName();
            dialogs.displayError(message, ex);
        }
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

    private class SubmissionCallback implements FutureCallback<SubmissionResult> {

        private Exercise exercise;
        private SubmissionResultWaitingDialog dialog;
        private TestResultDisplayer resultDisplayer;

        private SubmissionCallback(Exercise exercise, SubmissionResultWaitingDialog dialog) {
            SubmissionCallback.this.exercise = exercise;
            SubmissionCallback.this.dialog = dialog;
            SubmissionCallback.this.resultDisplayer = TestResultDisplayer.getInstance();
        }

        @Override
        public void onSuccess(final SubmissionResult result) {
            if(result == null){
                System.err.println("Result is null.");
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    dialog.close();
                    final ResultCollector resultCollector = new ResultCollector(exercise);
                    resultCollector.setValidationResult(result.getValidationResult());
             
                    resultDisplayer.showSubmissionResult(exercise, result, resultCollector);
                    exercise.setAttempted(true);
                    if (result.isAllTestsPassed()) {
                        exercise.setCompleted(true);
                    }
                    courseDb.save();
                    new CheckForNewExercisesOrUpdates(true, false).run();
                }

            });
        }

        @Override
        public void onFailure(final Throwable ex) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    Logger log = Logger.getLogger(SubmitExerciseAction.class.getName());
                    log.log(Level.INFO, "Error waiting for results from server.", ex);
                    String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                    dialogs.displayError("Error trying to get test results.", ex);
                    dialog.close();
                }

            });
        }

    }

}

