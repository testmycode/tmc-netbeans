package fi.helsinki.cs.tmc.utilities.exercise;

import java.io.InputStream;
import fi.helsinki.cs.tmc.data.Exercise;

/**
 * Used for listening when an exercise has been uploaded.
 * @author knordman
 */
public interface IExerciseUploadListener {

    public void exerciseUploadCompleted(Exercise exercise, InputStream response);

    public void exerciseUploadFailed(String errorMessage);
}
