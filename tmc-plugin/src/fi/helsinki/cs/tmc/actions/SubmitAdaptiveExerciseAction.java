package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.AdaptiveExerciseResultDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.SubmitAdaptiveExerciseAction")
@ActionRegistration(displayName = "#CTL_SubmitAdaptiveExerciseAction")
@ActionReference(path = "Menu/TM&C", position = -250)
@Messages("CTL_SubmitAdaptiveExerciseAction=Submit Adaptive Exercise")
public final class SubmitAdaptiveExerciseAction implements ActionListener {

    private static final Logger logger = Logger.getLogger(SubmitAdaptiveExerciseAction.class.getName());
    private final ConvenientDialogDisplayer dialogs;
    private final Project context;
    private final ProjectMediator projectMediator;
    private final TmcEventBus eventBus;
    private final CourseDb courseDb;
    private final ConvenientDialogDisplayer dialogDisplayer;
    private final TestResultDisplayer resultDisplayer;

    public SubmitAdaptiveExerciseAction(Project context) {
        this.context = context;
        this.courseDb = CourseDb.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        logger.log(Level.WARNING, "Init submit adaptive exercise.");

        ProgressObserver observer = new BridgingProgressObserver();
        TmcProjectInfo adaptiveProject = projectMediator.wrapProject(this.context);

        final String exerciseName = adaptiveProject.getProjectName();
        Callable<SubmissionResult> callable = TmcCore.get().submitAdaptiveExercise(observer, exerciseName);

            BgTask.start("Waiting for results from skillifier", callable, new BgTaskListener<SubmissionResult>() {
            @Override
            public void bgTaskReady(SubmissionResult result) {
                new AdaptiveExerciseResultDialog(exerciseName, result);
            }

            @Override
            public void bgTaskCancelled() {
                logger.log(Level.WARNING, "Adaptive submit cancelled.");
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.SEVERE, "Submitting adaptive exercise failed!");
                dialogDisplayer.displayError("Error trying to get test results.", ex);
            }

        });
    }
}
