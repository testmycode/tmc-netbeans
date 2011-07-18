package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A collection of Exercises.
 * @author jmturpei
 */
public class ExerciseCollection implements Iterable<Exercise> {

    /**
     * Course that owns this collection of exercises.
     */
    private Course course;
    private ArrayList<Exercise> items;

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

    /**
     * 
     * @return The course that owns this collection of exercises.
     */
    @Deprecated // TODO: merge this class into Course
    public Course getCourse() {
        return this.course;
    }

    //TODO: rename to addExercise
    public void add(Exercise item) {
        if (item == null) {
            throw new NullPointerException("item was null at CollectionBase.add");
        }
        processItem(item);
        items.add(item);
    }

    @Deprecated
    public void remove(Exercise item) {
        if (item == null) {
            throw new NullPointerException("item was null at CollectionBase.remove");
        }
        items.remove(item);
    }

    @Deprecated
    public int size() {
        return items.size();
    }

    @Deprecated
    public Exercise getItem(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("index was invalid at CollectionBase.getItem");
        }
        return items.get(index);
    }

    @Override
    public Iterator<Exercise> iterator() {
        return items.iterator();
    }
}
