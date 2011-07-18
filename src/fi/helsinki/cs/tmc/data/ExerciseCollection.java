package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list of Exercises associated with a course.
 */
public class ExerciseCollection extends ArrayList<Exercise> {

    /**
     * Course that owns this collection of exercises.
     */
    private Course course;

    public ExerciseCollection(Course course) {
        if (course == null) {
            throw new NullPointerException("course was null at ExerciseCollection.Constructor");
        }
        this.course = course;
    }
    
    public Course getCourse() {
        return this.course;
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
        processNewExercise(e);
        return super.add(e);
    }

    @Override
    public void add(int index, Exercise element) {
        processNewExercise(element);
        super.add(index, element);
    }
    
    @Override
    public boolean addAll(Collection<? extends Exercise> c) {
        for (Exercise e : c) {
            processNewExercise(e);
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Exercise> c) {
        for (Exercise e : c) {
            processNewExercise(e);
        }
        return super.addAll(index, c);
    }
    
    /**
     * Changes the given exercise's Course to this collection's course.
     * @param item 
     */
    protected void processNewExercise(Exercise item) {
        if (item == null) {
            throw new NullPointerException("exercise was null at ExerciseCollection.processItem");
        }

        if (getExerciseByName(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate exercise: " + item.getName() + " course:" + course.getName());
        }

        item.setCourse(course);
    }
}
