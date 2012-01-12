package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.openide.filesystems.FileUtil;

public class UpdateExercisesAction implements ActionListener {

    private List<Exercise> exercisesToUpdate;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ServerAccess serverAccess;
    private ConvenientDialogDisplayer dialogDisplayer;
    private NbProjectUnzipper unzipper;
    
    public UpdateExercisesAction(List<Exercise> exercisesToUpdate) {
        this.exercisesToUpdate = exercisesToUpdate;
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.serverAccess = new ServerAccess();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.unzipper = NbProjectUnzipper.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        for (final Exercise exercise : exercisesToUpdate) {
            final File projectDir = projectMediator.getProjectDirForExercise(exercise);
            BgTask.start("Downloading " + exercise.getName(), serverAccess.getDownloadingExerciseZipTask(exercise), new BgTaskListener<byte[]>() {

                @Override
                public void bgTaskReady(byte[] data) {
                    try {
                        unzipper.unzipProject(data, projectDir, exercise.getName(), overwritingDecider);
                    } catch (IOException ex) {
                        dialogDisplayer.displayError("Failed to update project.", ex);
                        return;
                    }
                    courseDb.exerciseDownloaded(exercise);
                    
                    // Refresh NB's file cache like "Source -> Scan for External Changes".
                    try {
                        FileUtil.toFileObject(projectDir).getFileSystem().refresh(true);
                    } catch (Exception e) {
                    }
                    
                    TmcProjectInfo project = projectMediator.tryGetProjectForExercise(exercise);
                    if (project != null) {
                        projectMediator.openProject(project);
                    }
                }

                @Override
                public void bgTaskCancelled() {
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    dialogDisplayer.displayError("Failed to update exercises", ex);
                }
            });
        }
    }
    
    private final NbProjectUnzipper.OverwritingDecider overwritingDecider = new NbProjectUnzipper.OverwritingDecider() {
        @Override
        public boolean canOverwrite(String relPath) {
            String s = File.separator;
            return relPath.startsWith("test") ||
                    relPath.startsWith("lib") ||
                    (relPath.startsWith("nbproject") && !relPath.startsWith("nbproject" + s + "private")) ||
                    !relPath.contains(s); // i.e. a file in the project's root dir
        }
    };
    
}
