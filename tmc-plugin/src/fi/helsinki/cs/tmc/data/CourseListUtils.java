package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.core.domain.Course;

import java.util.List;

public class CourseListUtils {
    /**
     * Returns the course with the given name or null if not found.
     */
    public static Course getCourseByName(List<Course> courses, String courseName) {
        for (Course course : courses) {
            if (course.getName().equals(courseName)) {
                return course;
            }
        }

        return null;
    }
    
    public static boolean hasCourseByName(List<Course> courses, String courseName) {
        return getCourseByName(courses, courseName) != null;
    }
}
