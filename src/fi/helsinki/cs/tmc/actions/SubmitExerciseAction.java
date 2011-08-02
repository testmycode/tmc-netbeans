package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.netbeans.api.project.Project;
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

    private List<Project> projects;
    private ServerAccess serverAccess;
    private LocalCourseCache courseCache;
    private ProjectMediator projectMediator;
    private SubmissionResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private ExerciseIconAnnotator iconAnnotator;

    public SubmitExerciseAction(List<Project> projects) {
        this(projects,
                ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                SubmissionResultDisplayer.getInstance(),
                ConvenientDialogDisplayer.getDefault(),
                Lookup.getDefault().lookup(ExerciseIconAnnotator.class));
    }

    /*package*/ SubmitExerciseAction(
            List<Project> projects,
            ServerAccess serverAccess,
            LocalCourseCache courseCache,
            ProjectMediator projectMediator,
            SubmissionResultDisplayer resultDisplayer,
            ConvenientDialogDisplayer dialogDisplayer,
            ExerciseIconAnnotator iconAnnotator) {
        this.projects = projects;
        this.serverAccess = serverAccess;
        this.courseCache = courseCache;
        this.projectMediator = projectMediator;
        this.resultDisplayer = resultDisplayer;
        this.dialogDisplayer = dialogDisplayer;
        this.iconAnnotator = iconAnnotator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (Project nbProject : projects) {
            TmcProjectInfo tmcProject = projectMediator.wrapProject(nbProject);
            submitProject(tmcProject);
        }
    }
    
    private void submitProject(TmcProjectInfo project) {
        final Exercise exercise = projectMediator.tryGetExerciseForProject(project, courseCache);
        if (exercise == null) {
            return;
        }
        
        projectMediator.saveAllFiles();

        serverAccess.startSubmittingExercise(exercise, new BgTaskListener<SubmissionResult>() {
            @Override
            public void backgroundTaskReady(SubmissionResult result) {
                resultDisplayer.showResult(result);
                exercise.setAttempted(true);
                if (result.getStatus() == SubmissionResult.Status.OK) {
                    exercise.setCompleted(true);
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
