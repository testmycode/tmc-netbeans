package palikka.utilities.exercise;

import java.io.File;
import java.util.Date;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileUtil;
import palikka.data.Course;
import palikka.data.Exercise;
import palikka.data.ExerciseCollection;
import palikka.utilities.FolderHelper;

/**
 * This class is used to filter exercises based on deadline, wether they are
 * local or not (on disk) and wether they are downloadable.
 * @author ttkoivis
 */
public class ExerciseFilter {

    /**
     * Constructor
     */
    public ExerciseFilter() {
    }

    /**
     * Filter the given ExerciseCollection based on the given date.
     * @param e Collection of exercises to be filtered
     * @param currentTime
     * @return A new filtered ExerciseCollection
     */
    public static ExerciseCollection getNonExpired(ExerciseCollection e, Date currentTime) {
        ExerciseCollection collection = new ExerciseCollection(e.getCourse());

        for (Exercise exercise : e) {
            if (!exercise.isDeadlineEnded(currentTime)) {
                collection.add(new Exercise(exercise));
            }
        }

        return collection;
    }

    /**
     * Used to get the exercises currently on disk from the given
     * ExerciseCollection.
     * @param e
     * @return A new filtered ExerciseCollection
     */
    public static ExerciseCollection getLocal(ExerciseCollection e) {
        ExerciseCollection collection = new ExerciseCollection(e.getCourse());
        Course course = e.getCourse();

        for (Exercise exercise : e) {
            if (isExerciseLocal(exercise)) {
                collection.add(new Exercise(exercise));
            }
        }

        return collection;
    }

    /**
     * Filters an ExerciseCollection so that it returns an ExerciseCollection
     * that has the exercises that haven't been downloaded yet.
     * @param e
     * @return A new filtered ExerciseCollection
     */
    public static ExerciseCollection getDownloadable(ExerciseCollection e) {
        ExerciseCollection collection = new ExerciseCollection(e.getCourse());

        for (Exercise exercise : e) {
            if (!isExerciseLocal(exercise)) {
                collection.add(new Exercise(exercise));
            }
        }

        return collection;
    }

    /**
     * method checks if folder of given exercise exists.
     * @param exercise
     * @return 
     */
    private static boolean isExerciseLocal(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise in ExerciseInspector.isExerciseLocal was null!");
        }

        File path = FolderHelper.searchNbProject(exercise);
        //JOptionPane.showConfirmDialog(null, path.getAbsolutePath());
        if (path == null) {
            return false;
        }

        return ProjectManager.getDefault().isProject(FileUtil.toFileObject(FileUtil.normalizeFile(path)));

    }
}
