package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;

/**
 * A list of Exercises.
 */
public class ExerciseCollection extends ArrayList<Exercise> {
    
    /**
     * Returns the exercise with the given name or null if not found.
     */
    public Exercise getExerciseByName(String exerciseName) {
        if (exerciseName == null) {
            return null;
        }

        for (Exercise exercise : this) {
            if (exercise.getName().equals(exerciseName)) {
                return exercise;
            }
        }

        return null;
    }
}
