package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;

/**
 * A CourseCollection is a collection of Course objects.
 * @author kkaltiai
 */
public class CourseCollection extends ArrayList<Course> {
    /**
     * Returns the course with the given name or null if not found.
     */
    public Course getCourseByName(String courseName) {
        if (courseName == null) {
            return null;
        }

        for (Course course : this) {
            if (course.getName().equals(courseName)) {
                return course;
            }
        }

        return null;
    }
    
    @Override
    public boolean add(Course item) {
        processItem(item); // TODO: get rid of this if possible.
        return super.add(item);
    }

    /**
     * Method is called whenever a new object is added to this collection.
     * The method checks that the item is not null and that there are no duplicates.
     * @param item 
     */
    @Deprecated
    protected void processItem(Course item) {
        if (item == null) {
            throw new NullPointerException("course was null at CourseCollection.processItem");
        }

        if (getCourseByName(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate course: " + item.getName());
        }
    }
}