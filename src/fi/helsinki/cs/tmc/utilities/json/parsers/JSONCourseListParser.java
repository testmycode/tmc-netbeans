package fi.helsinki.cs.tmc.utilities.json.parsers;

import com.google.gson.Gson;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;

public class JSONCourseListParser {
    
    /**
     * Creates a CourseCollection object from JSON.
     */
    public static CourseCollection parseJson(String json) {
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
    }
    
    /**
     * Method checks if String json parameter is in proper json form 
     */
    @Deprecated
    public static boolean isValidJson(String json) {
        try {
            parseJson(json);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
