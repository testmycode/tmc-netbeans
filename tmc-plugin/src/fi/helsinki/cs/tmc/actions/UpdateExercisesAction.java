package fi.helsinki.cs.tmc.actions;

import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ExerciseUpdateOverwritingDecider;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class UpdateExercisesAction implements ActionListener {
    
    private static final Logger log = Logger.getLogger(UpdateExercisesAction.class.getName());

    private List<Exercise> exercisesToUpdate;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ServerAccess serverAccess;
    private ConvenientDialogDisplayer dialogDisplayer;
    
    public UpdateExercisesAction(List<Exercise> exercisesToUpdate) {
        this.exercisesToUpdate = exercisesToUpdate;
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.serverAccess = new ServerAccess();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        final AggregatingBgTaskListener<TmcProjectInfo> projectOpener = new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToUpdate.size(), new BgTaskListener<Collection<TmcProjectInfo>>() {
            @Override
            public void bgTaskReady(Collection<TmcProjectInfo> result) {
                result = new ArrayList<TmcProjectInfo>(result);
                
                // result may contain nulls since some downloads might fail
                while (result.remove(null)) {
                }
                
                projectMediator.scanForExternalChanges(result);
                
                // Open all at once. This is much faster.
                projectMediator.openProjects(result);
            }

            // Cancelled and failed are never called since we only call bgTaskReady below manually
            @Override
            public void bgTaskCancelled() {
            }
            @Override
            public void bgTaskFailed(Throwable ex) {
            }
        });
        
        
        for (final Exercise exercise : exercisesToUpdate) {
            final File projectDir = projectMediator.getProjectDirForExercise(exercise);
            
            BgTask.start("Downloading " + exercise.getName(), serverAccess.getDownloadingExerciseZipTask(exercise), new BgTaskListener<byte[]>() {

                @Override
                public void bgTaskReady(byte[] data) {
                    TmcProjectInfo project = null;
                    try {
                        try {
                            ExerciseUpdateOverwritingDecider overwriter = new ExerciseUpdateOverwritingDecider(projectDir);
                            NbProjectUnzipper unzipper = new NbProjectUnzipper(overwriter);
                            NbProjectUnzipper.Result result = unzipper.unzipProject(data, projectDir);
                            log.info("== Exercise unzip result ==\n" + result);
                        } catch (IOException ex) {
                            dialogDisplayer.displayError("Failed to update project.", ex);
                            return;
                        }
                        courseDb.exerciseDownloaded(exercise);
                        
                        project = projectMediator.tryGetProjectForExercise(exercise);
                    } finally {
                        projectOpener.bgTaskReady(project);
                    }
                }

                @Override
                public void bgTaskCancelled() {
                    projectOpener.bgTaskReady(null);
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    projectOpener.bgTaskReady(null);
                    String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                    dialogDisplayer.displayError("Failed to download updated exercises.\n" + msg, ex);
                }
            });
        }
    }
}
