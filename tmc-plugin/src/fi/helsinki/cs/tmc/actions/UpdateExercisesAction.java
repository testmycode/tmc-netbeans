package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

public class UpdateExercisesAction implements ActionListener {

    private static final Logger log = Logger.getLogger(UpdateExercisesAction.class.getName());

    private List<Exercise> exercisesToUpdate;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ServerAccess serverAccess;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcCore core;

    public UpdateExercisesAction(List<Exercise> exercisesToUpdate) {
        this.exercisesToUpdate = exercisesToUpdate;
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.serverAccess = new ServerAccess();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.core = TmcCoreSingleton.getInstance();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        try {
            ListenableFuture<List<Exercise>> downloadFuture = core.downloadExercises(exercisesToUpdate,
                    NBTmcSettings.getDefault());
            Futures.addCallback(downloadFuture, new ProjectOpenerCallback());
            
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
            dialogDisplayer.displayError("Error occured while downloading updates", ex);
        }
    }

    private class ProjectOpenerCallback implements FutureCallback<List<Exercise>> {

        @Override
        public void onSuccess(List<Exercise> downloadedExercises) {
            ArrayList<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>();

            for (Exercise exercise : downloadedExercises) {
                courseDb.exerciseDownloaded(exercise);
                TmcProjectInfo project = projectMediator.tryGetProjectForExercise(exercise);
                if (project != null) {
                    projects.add(project);
                }
            }
            projectMediator.scanForExternalChanges(projects);

            projectMediator.openProjects(projects);
        }

        @Override
        public void onFailure(Throwable ex) {
            Exceptions.printStackTrace(ex);
            dialogDisplayer.displayError("Error occured while downloading updates", ex);
        }

    }
}
