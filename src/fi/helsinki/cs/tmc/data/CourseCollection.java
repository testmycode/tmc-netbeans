package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.Refactored;
import java.util.ArrayList;

/**
 * A list of Courses.
 */
@Refactored
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
}