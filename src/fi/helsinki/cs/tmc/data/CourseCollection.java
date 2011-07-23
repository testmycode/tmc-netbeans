package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;

/**
 * A list of Courses.
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
    
    public boolean hasCourseByName(String courseName) {
        return getCourseByName(courseName) != null;
    }
}