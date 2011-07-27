package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseProgress;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.SubmitExerciseAction")
@ActionRegistration(iconBase = "fi/helsinki/cs/tmc/actions/submit.png",
displayName = "#CTL_SubmitExerciseAction", iconInMenu=false)
@ActionReferences({
    @ActionReference(path = "Menu/TMC", position = 20),
    @ActionReference(path = "Toolbars/TMC", position = 200)
})
@Messages("CTL_SubmitExerciseAction=Submit")
public final class SubmitExerciseAction implements ActionListener {

    private ServerAccess serverAccess;
    private LocalCourseCache courseCache;
    private ProjectMediator projectMediator;
    private SubmissionResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private ExerciseIconAnnotator iconAnnotator;

    public SubmitExerciseAction() {
        this(ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                SubmissionResultDisplayer.getInstance(),
                ConvenientDialogDisplayer.getDefault(),
                Lookup.getDefault().lookup(ExerciseIconAnnotator.class));
    }

    public SubmitExerciseAction(
            ServerAccess serverAccess,
            LocalCourseCache courseCache,
            ProjectMediator projectMediator,
            SubmissionResultDisplayer resultDisplayer,
            ConvenientDialogDisplayer dialogDisplayer,
            ExerciseIconAnnotator iconAnnotator) {
        this.serverAccess = serverAccess;
        this.courseCache = courseCache;
        this.projectMediator = projectMediator;
        this.resultDisplayer = resultDisplayer;
        this.dialogDisplayer = dialogDisplayer;
        this.iconAnnotator = iconAnnotator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmcProjectInfo project = projectMediator.getMainProject();
        if (project == null) {
            return;
        }
        final Exercise exercise = projectMediator.tryGetExerciseForProject(project, courseCache);
        if (exercise == null) {
            return;
        }
        
        projectMediator.saveAllFiles();

        serverAccess.startSubmittingExercise(exercise, new BgTaskListener<SubmissionResult>() {
            @Override
            public void backgroundTaskReady(SubmissionResult result) {
                resultDisplayer.showResult(result);
                if (result.getStatus() == SubmissionResult.Status.OK) {
                    exercise.setProgress(ExerciseProgress.DONE);
                } else {
                    exercise.setProgress(ExerciseProgress.PARTIALLY_DONE);
                }
                iconAnnotator.updateAllIcons();
                courseCache.save();
            }

            @Override
            public void backgroundTaskCancelled() {
            }

            @Override
            public void backgroundTaskFailed(Throwable ex) {
                dialogDisplayer.displayError(ex);
            }
        });
    }
}
