package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Downloads and opens the given exercises in the background.
 */
public class DownloadExercisesAction {
    private static final Logger logger = Logger.getLogger(DownloadExercisesAction.class.getName());

    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    private List<Exercise> exercisesToDownload;

    public DownloadExercisesAction(List<Exercise> exercisesToOpen) {
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        this.exercisesToDownload = exercisesToOpen;
    }

    public void run() {
        final AggregatingBgTaskListener<TmcProjectInfo> aggregator =
                new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToDownload.size(), whenAllDownloadsFinished);

        for (final Exercise exercise : exercisesToDownload) {
            startDownloading(exercise, aggregator);
        }
    }

    private void startDownloading(final Exercise exercise, final BgTaskListener<TmcProjectInfo> listener) {
        BgTask.start("Downloading " + exercise.getName(), serverAccess.getDownloadingExerciseZipTask(exercise), new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(final byte[] zipData) {
                BgTask.start("Extracting project", new Callable<TmcProjectInfo>() {
                    @Override
                    public TmcProjectInfo call() throws Exception {
                        NbProjectUnzipper unzipper = new NbProjectUnzipper();
                        unzipper.unzipProject(zipData, projectMediator.getProjectDirForExercise(exercise));
                        TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(exercise);
                        if (proj == null) {
                            throw new RuntimeException("Failed to open project for exercise " + exercise.getName());
                        }

                        // Need to invoke courseDb in swing thread to avoid races
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                courseDb.exerciseDownloaded(exercise);
                            }
                        });

                        return proj;
                    }
                }, listener);
            }

            @Override
            public void bgTaskCancelled() {
                listener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                listener.bgTaskFailed(ex);
            }
        });
    }

    private BgTaskListener<Collection<TmcProjectInfo>> whenAllDownloadsFinished = new BgTaskListener<Collection<TmcProjectInfo>>() {
        @Override
        public void bgTaskReady(Collection<TmcProjectInfo> projects) {
            projectMediator.openProjects(projects);
        }

        @Override
        public void bgTaskCancelled() {
        }

        @Override
        public void bgTaskFailed(Throwable ex) {
            logger.log(Level.INFO, "Failed to download exercise file.", ex);
            dialogs.displayError("Failed to download exercises.\n" + DownloadErrorHelper.getDownloadExceptionMsg(ex));
        }
    };
}
