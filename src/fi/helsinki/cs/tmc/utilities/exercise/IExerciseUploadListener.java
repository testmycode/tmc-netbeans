package fi.helsinki.cs.tmc.utilities.exercise;

import java.io.InputStream;
import fi.helsinki.cs.tmc.data.Exercise;

/**
 * Used for listening when an exercise has been uploaded.
 * @author knordman
 */
public interface IExerciseUploadListener {

    public void ExerciseUploadCompleted(Exercise exercise, InputStream response);

    public void ExerciseUploadFailed(String errorMessage);
}
