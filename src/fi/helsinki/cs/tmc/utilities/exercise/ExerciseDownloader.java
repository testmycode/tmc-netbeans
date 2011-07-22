package fi.helsinki.cs.tmc.utilities.exercise;

import fi.helsinki.cs.tmc.utilities.http.FileDownloaderAsync;
import fi.helsinki.cs.tmc.utilities.http.IDownloadListener;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.settings.LegacyPluginSettings;

/**
 * This class is used to download exercises given as a single Exercise object or
 * an ExerciseCollection.
 * @author jmturpei
 */
public class ExerciseDownloader implements IDownloadListener {

    private IExerciseDownloadListener listener;
    /**
     * Collection of exercises to be downloaded
     */
    private ExerciseCollection exerciseCollection;
    private boolean downloadStarted = false;
    /**
     * The exercise currently being downloaded.
     */
    private Exercise currentExercise;
    /**
     * The downloader used to download the exercises.
     */
    private FileDownloaderAsync currentDownloader;
    private boolean cancelAll;

    /**
     * Constructor
     * @param exerciseCollection Collection of exercises to be downloaded
     * @param listener Interface that listens to this ExerciseDownloader
     */
    public ExerciseDownloader(ExerciseCollection exerciseCollection, IExerciseDownloadListener listener) {
        if (exerciseCollection == null) {
            throw new NullPointerException("exerciseCollection is null at ExerciseDownloader.Constructor");
        }
        if (listener == null) {
            throw new NullPointerException("listener is null at ExerciseDownloader.Constructor");
        }

        this.listener = listener;
        this.exerciseCollection = exerciseCollection;
    }

    /**
     * Used to cancel all downloads. The current download can't be interrupted
     * but calling this will prevent the downloader from downloading any more
     * exercises and the current download will not send messages to the
     * Controller.
     */
    public void cancelAll() {
        cancelAll = true;
    }

    /**
     * Tell this ExerciseDownloader to download the given exercises.
     * @throws Exception 
     */
    public void downloadExercises() throws Exception {
        if (downloadStarted) {
            return;
        }
        downloadStarted = true;

        scheduleNextdownload();
    }

    /**
     * Calling this method gives a new exercise to the downloader to download or
     * if there are no more downloadable exercises, stops the downloading.
     */
    private void scheduleNextdownload() {
        if (exerciseCollection.isEmpty() || cancelAll) {
            listener.exerciseDownloadCompleted();
            return;
        }


        currentExercise = exerciseCollection.remove(0);

        try {

            currentDownloader = new FileDownloaderAsync(currentExercise.getDownloadAddress(), this);
            currentDownloader.setTimeout(LegacyPluginSettings.getSettings().getExerciseDownloadTimeout());

            currentDownloader.download("Downloading exercise " + currentExercise.getName());

        } catch (Exception e) {
            listener.exerciseDownloadFailed("internal error. Failed to create FileDownloaderAsync " + e.getMessage());
        }


    }

    /**
     * Called by the downloader when a download finishes. this method then
     * calls the scheduleNextDownload().
     * @param source 
     */
    @Override
    public void downloadCompleted(FileDownloaderAsync source) {

        Exercise downloadedExercise = currentExercise;

        listener.exerciseDownloadCompleted(downloadedExercise, source.getFileContent());

        scheduleNextdownload();
    }

    /**
     * Called by the downloader when a download fails. The method then
     * calls ScheduleNextDownload().
     * @param source 
     */
    @Override
    public void downloadFailed(FileDownloaderAsync source) {


        listener.exerciseDownloadFailed(source.getErrorMsg());

        scheduleNextdownload();

    }

    /**
     * Called by the downloader when the download was cancelled by user. 
     * The method then calls ScheduleNextDownload().
     * @param source 
     */
    @Override
    public void downloadCancelledByUser(FileDownloaderAsync source) {

        Exercise cancelledExercise = currentExercise;

        listener.exerciseDownloadCancelledByUser(cancelledExercise);

        scheduleNextdownload();
    }
}
