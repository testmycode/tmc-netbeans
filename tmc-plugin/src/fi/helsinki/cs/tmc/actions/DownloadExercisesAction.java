package fi.helsinki.cs.tmc.actions;

import static java.util.logging.Level.INFO;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.model.CourseDb;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Downloads and opens the given exercises in the background.
 */
public class DownloadExercisesAction {

    private static final Logger logger = Logger.getLogger(DownloadExercisesAction.class.getName());

    private final List<Exercise> exercisesToDownload;
    private final TmcCore tmcCore;
    private final NbTmcSettings settings;
    private CourseDb courseDb;

    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    /**
     * Downloads all exercises of the list from TmcServer, unzips and opens them
     * and saves the checksums of each Exercise to courseDb.
     */
    public DownloadExercisesAction(List<Exercise> exercisesToOpen) {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.courseDb = CourseDb.getInstance();

        this.exercisesToDownload = exercisesToOpen;
        this.tmcCore = TmcCoreSingleton.getInstance();
        this.settings = NbTmcSettings.getDefault();
    }

    public void run() {
//        final AggregatingBgTaskListener<TmcProjectInfo> aggregator
//                = new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToDownload.size(), whenAllDownloadsFinished);

        for (final Exercise exercise : exercisesToDownload) {
            startDownloading(exercise, whenDownloadsFinished);
        }
    }

    private void startDownloading(final Exercise exercise, final BgTaskListener<Collection<Exercise>> listener) {
        BgTask.start("Downloading " + exercise.getName(), new CancellableCallable<List<Exercise>>() {

            ListenableFuture<List<Exercise>> dlFuture;

            @Override
            public List<Exercise> call() throws Exception {
                dlFuture = tmcCore.downloadExercises(Lists.newArrayList(exercise));

                return dlFuture.get();
            }

            @Override
            public boolean cancel() {
                logger.info("Exercise download was cancelled.");
                return dlFuture.cancel(true);
            }
        }, listener);
    }

    private BgTaskListener<Collection<Exercise>> whenDownloadsFinished = new BgTaskListener<Collection<Exercise>>() {
        @Override
        public void bgTaskReady(Collection<Exercise> exercises) {
            logger.log(INFO, "1Opening projects.");
            List<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>(exercises.size());
            for (Exercise ex : exercises) {
                projects.add(projectMediator.tryGetProjectForExercise(ex));
            }
            projectMediator.openProjects(projects);
            logger.log(INFO, "2Opening projects.");
        }

        @Override
        public void bgTaskCancelled() {
            logger.log(INFO, "BgTask was cancelled.");
        }

        @Override
        public void bgTaskFailed(Throwable ex) {
            logger.log(INFO, "Failed to download exercise file.", ex);
            dialogs.displayError("Failed to download exercises.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
        }
    };
}
