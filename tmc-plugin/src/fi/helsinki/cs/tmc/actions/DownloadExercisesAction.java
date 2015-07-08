package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads and opens the given exercises in the background.
 */
public class DownloadExercisesAction {

    private static final Logger logger = Logger.getLogger(DownloadExercisesAction.class.getName());

    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    private List<Exercise> exercisesToDownload;
    private TmcCore tmcCore;
    private NBTmcSettings settings;

    public DownloadExercisesAction(List<Exercise> exercisesToOpen) {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        this.exercisesToDownload = exercisesToOpen;
        this.tmcCore = TmcCoreSingleton.getInstance();
        this.settings = NBTmcSettings.getDefault();
    }

    public void run() throws TmcCoreException {
        // final AggregatingBgTaskListener<TmcProjectInfo> aggregator
        //         = new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToDownload.size(), whenAllDownloadsFinished);

        ListenableFuture<List<Exercise>> dlFuture = tmcCore.donwloadExercises(exercisesToDownload, settings);

        Futures.addCallback(dlFuture, new ProjectOpener());
    }

    private class ProjectOpener implements FutureCallback<List<Exercise>> {

        @Override
        public void onSuccess(List<Exercise> downloadedExercises) {
            List<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>();
            for (Exercise exercise : downloadedExercises) {
                TmcProjectInfo info = projectMediator.tryGetProjectForExercise(exercise);
                if (info == null) {
                    dialogs.displayError(
                            "Could not find exercise " + exercise.getName() + " from the filesystem");
                    continue;
                }
                projects.add(info);
            }
            projectMediator.openProjects(projects);
        }

        @Override
        public void onFailure(Throwable thrwbl) {
            logger.log(Level.INFO, "Failed to download exercise file.", thrwbl);
            dialogs.displayError("Failed to download exercises.\n" + ServerErrorHelper.getServerExceptionMsg(thrwbl));

        }
    }
}
