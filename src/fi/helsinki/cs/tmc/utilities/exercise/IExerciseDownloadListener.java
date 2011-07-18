package fi.helsinki.cs.tmc.utilities.exercise;

import java.io.InputStream;
import fi.helsinki.cs.tmc.data.Exercise;

/**
 * Used to listen when an exercise is downloaded.
 * @author jmturpei
 */
public interface IExerciseDownloadListener {
    
    public void exerciseDownloadCompleted(Exercise downloadedExercise, InputStream fileContent);
    public void exerciseDownloadFailed(String errorMsg);
    public void exerciseDownloadCancelledByUser(Exercise cancelledExercise);     
    public void exerciseDownloadCompleted();
    
}
