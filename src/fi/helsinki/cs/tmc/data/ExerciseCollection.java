package fi.helsinki.cs.tmc.data;

/**
 * A collection of Exercises.
 * @author jmturpei
 */
public class ExerciseCollection extends CollectionBase<Exercise> {

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

    /**
     * Searches if it can find an Exercise object by given name
     * @param exerciseName
     * @return null if it cannot find the Exercise by given exerciseName or if 
     * given exerciseName parameter is null. Otherwise it returns a reference to the exercise.
     */
    public Exercise searchExercise(String exerciseName) {
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
    @Override
    protected void processItem(Exercise item) {
        if (item == null) {
            throw new NullPointerException("exercise was null at ExerciseCollection.processItem");
        }

        if (searchExercise(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate exercise: " + item.getName() + " course:" + course.getName());
        }

        item.setCourse(course);
    }

    /**
     * 
     * @return The course that owns this collection of exercises.
     */
    public Course getCourse() {
        return this.course;
    }
}
