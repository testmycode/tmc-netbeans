package palikka.utilities.json.parsers;

import palikka.utilities.json.parsers.jsonorg.JSONArray;
import palikka.utilities.json.parsers.jsonorg.JSONException;
import palikka.utilities.json.parsers.jsonorg.JSONObject;
import palikka.data.Course;
import palikka.data.CourseCollection;

/**
 *
 * @author jmturpei
 */
public class JSONCourseListParser {

    /**
     * Creates a CourseCollection object from json String parameter. 
     * @param json String
     * @return reference to courseCollection 
     */
    public static CourseCollection parseJson(String json) throws JSONException, NullPointerException {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }

        CourseCollection courseCollection = new CourseCollection();

        try {
            JSONArray jsonCourses = new JSONArray(json);

            for (int i = 0; i < jsonCourses.length(); i++) {

                JSONObject jsonCourse = jsonCourses.getJSONObject(i).getJSONObject("course");
                Course course = createCourse(jsonCourse);

                courseCollection.add(course);
            }
        } catch (JSONException e) {
            throw new JSONException("invalid JSON String!");
        }

        return courseCollection;
    }
    
    /**
     * Method checks if String json parameter is in proper json form 
     * @param json String
     * @return true or false boolean
     */
    public static boolean isValidJson(String json) {
        try {
            parseJson(json);
        } catch (JSONException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        return true;
    }

    /**
     * Creates a Course object from JSONObject.  
     * @param jsonCourse JSONObject
     * @return course Course
     */
    private static Course createCourse(JSONObject jsonCourse) throws JSONException {
        Course course = new Course();

        try {
            course.setName(jsonCourse.getString("name"));
            course.setExerciseListDownloadAddress(jsonCourse.getString("exercises_json"));


        } catch (JSONException e) {
            throw new JSONException("invalid JSONObject!");
        }

        return course;
    }
}
