package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DownloadAdaptiveExerciseAction {

    private static final Logger logger = Logger.getLogger(DownloadAdaptiveExerciseAction.class.getName());

    private CourseDb courseDb;
    private final ProjectMediator projectMediator;

    public DownloadAdaptiveExerciseAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
    }

    public void downloadAdaptiveExercise() {
        logger.log(Level.INFO, "Init adaptive exercise downloading");
        ProgressObserver observer = new BridgingProgressObserver();
        int week = courseDb.getAdaptiveWeek();

        Callable<Exercise> ex = TmcCore.get().downloadAdaptiveExercise(observer, week, courseDb.getCurrentCourse());
        BgTask.start("Downloading new adaptive exercise...", ex, observer, new BgTaskListener<Exercise>() {
            @Override
            public void bgTaskReady(final Exercise ex) {
                if (ex == null) {
                    logger.log(Level.INFO, "No adaptive exercises to download");
                    ConvenientDialogDisplayer.getDefault().displayMessage("No adaptive exercises to download");
                    return;
                }
                logger.log(Level.INFO, "Adaptive exercise downloaded");
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
                projectMediator.openProject(proj);

                if (proj == null) {
                    throw new RuntimeException("Failed to open project for exercise " + ex.getName());
                }

                // Need to invoke courseDb in swing thread to avoid races
                TmcSwingUtilities.ensureEdt(new Runnable() {
                    @Override
                    public void run() {
                        courseDb.exerciseDownloaded(ex);
                    }
                });
            }

            @Override
            public void bgTaskCancelled() {
                logger.log(Level.INFO, "Adaptive exercise download cancelled");
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.SEVERE, "Something went wrong: " + ex.toString(), ex);
            }
        });
    }
}
