package palikka.utilities.exercise;

import java.io.InputStream;
import palikka.data.Exercise;

/**
 * Used for listening when an exercise has been uploaded.
 * @author knordman
 */
public interface IExerciseUploadListener {

    public void ExerciseUploadCompleted(Exercise exercise, InputStream response);

    public void ExerciseUploadFailed(String errorMessage);
}
