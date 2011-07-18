package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;

/**
 * A collection of Exercises.
 * @author jmturpei
 */
public class ExerciseCollection extends ArrayList<Exercise> {

    /**
     * Course that owns this collection of exercises.
     */
    @Deprecated
    private Course course;

    public ExerciseCollection(Course course) {
        if (course == null) {
            throw new NullPointerException("course was null at ExerciseCollection.Constructor");
        }
        this.course = course;
    }

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

    @Override
    public boolean add(Exercise e) {
        processItem(e); //TODO: get rid of this if possible
        return super.add(e);
    }
    /**
     * Changes the given exercise's Course to this collection's course.
     * @param item 
     */
    @Deprecated
    protected void processItem(Exercise item) {
        if (item == null) {
            throw new NullPointerException("exercise was null at ExerciseCollection.processItem");
        }

        if (getExerciseByName(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate exercise: " + item.getName() + " course:" + course.getName());
        }

        item.setCourse(course);
    }

    @Deprecated // TODO: Course to point to this, not the other way around
    public Course getCourse() {
        return this.course;
    }
}
