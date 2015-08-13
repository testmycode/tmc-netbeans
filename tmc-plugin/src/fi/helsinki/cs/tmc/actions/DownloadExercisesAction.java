package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;

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

    /**
     * Downloads all exercises of the list from TmcServer, unzips and opens them and 
     * saves the checksums of each Exercise to courseDb.
     */
    public DownloadExercisesAction(List<Exercise> exercisesToOpen) {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        this.exercisesToDownload = exercisesToOpen;
        this.tmcCore = TmcCoreSingleton.getInstance();
        this.settings = NBTmcSettings.getDefault();
    }

    public void run() throws TmcCoreException {
        ProgressHandle exerciseDownload = ProgressHandleFactory.createSystemHandle(
                "Downloading " + exercisesToDownload.size() + " exercises.");
        exerciseDownload.start();
        ListenableFuture<List<Exercise>> dlFuture = tmcCore.downloadExercises(exercisesToDownload, settings);

        Futures.addCallback(dlFuture, new ProjectOpener(exerciseDownload));
    }

    private class ProjectOpener implements FutureCallback<List<Exercise>> {

        private ProgressHandle lastAction;
        
        /**
         * Converts Exercise objects to TmcProjectInfo objects.
         * Saves them to CourseDb and opens them.
         * @param lastAction 
         */
        public ProjectOpener(ProgressHandle lastAction) {
            this.lastAction = lastAction;
        }

        @Override
        public void onSuccess(List<Exercise> downloadedExercises) {
            lastAction.finish();
            List<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>();
            for (Exercise exercise : downloadedExercises) {
                TmcProjectInfo info = projectMediator.tryGetProjectForExercise(exercise);
                if (info == null) {
                    continue;
                }
                projects.add(info);
            }
            saveDownloadedExercisesToCourseDb(downloadedExercises);
            projectMediator.openProjects(projects);
        }

        private void saveDownloadedExercisesToCourseDb(final List<Exercise> downloadedExercises) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        CourseDb.getInstance().multipleExerciseDownloaded(downloadedExercises);
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void onFailure(Throwable thrwbl) {
            lastAction.finish();
            logger.log(Level.INFO, "Failed to download exercise file.", thrwbl);
            dialogs.displayError("Failed to download exercises.\n" + ServerErrorHelper.getServerExceptionMsg(thrwbl));
        }
    }
}
