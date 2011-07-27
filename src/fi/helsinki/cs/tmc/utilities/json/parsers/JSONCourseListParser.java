package fi.helsinki.cs.tmc.utilities.json.parsers;

import com.google.gson.Gson;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;

public class JSONCourseListParser {
    
    /**
     * Creates a CourseCollection object from JSON.
     */
    public static CourseCollection parseJson(String json) {
        try {
            if (json == null) {
                throw new NullPointerException("Json string is null");
            }

            Gson gson = new Gson();
            Course[] courses = gson.fromJson(json, Course[].class);

            CourseCollection courseCollection = new CourseCollection();
            for (Course course : courses) {
                courseCollection.add(course);
            }

            return courseCollection;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse course list: " + e.getMessage(), e);
        }
    }
}
