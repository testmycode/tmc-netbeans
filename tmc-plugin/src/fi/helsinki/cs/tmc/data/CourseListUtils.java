package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.core.domain.Course;

import java.util.List;

@Deprecated
public class CourseListUtils {
    /**
     * Returns the course with the given name or null if not found.
     */
    @Deprecated
    public static Course getCourseByName(List<Course> courses, String courseName) {
        for (Course course : courses) {
            if (course.getName().equals(courseName)) {
                return course;
            }
        }

        return null;
    }
    
    @Deprecated
    public static boolean hasCourseByName(List<Course> courses, String courseName) {
        return getCourseByName(courses, courseName) != null;
    }
}
