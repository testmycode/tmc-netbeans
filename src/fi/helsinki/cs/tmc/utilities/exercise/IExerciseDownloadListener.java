package fi.helsinki.cs.tmc.utilities.exercise;

import java.io.InputStream;
import fi.helsinki.cs.tmc.data.Exercise;

/**
 * Used to listen when an exercise is downloaded.
 * @author jmturpei
 */
public interface IExerciseDownloadListener {
    
    public void ExerciseDownloadCompleted(Exercise downloadedExercise, InputStream fileContent);
    public void ExerciseDownloadFailed(String errorMsg);
    public void ExerciseDownloadCancelledByUser(Exercise cancelledExercise);     
    public void ExercisedownloaderCompleted();
    
}
