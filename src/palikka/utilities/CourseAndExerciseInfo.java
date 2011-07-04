/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities;

import java.io.IOException;
import palikka.utilities.textio.ReadFromFile;
import palikka.data.Course;
import palikka.data.CourseCollection;
import palikka.data.ExerciseCollection;
import palikka.settings.PluginSettings;
import palikka.settings.Settings;
import palikka.utilities.json.parsers.JSONCourseListParser;
import palikka.utilities.json.parsers.JSONExerciseListParser;
import palikka.utilities.json.parsers.jsonorg.JSONException;

/**
 * 
 * @author knordman
 */
public class CourseAndExerciseInfo {

    /**
     * Used to get a CourseCollection of current courses from local JSON file.
     * @return CourseCollection with current courses or null if there is no JSON file available at the default folder.
     */
    public static CourseCollection getCourses() throws IOException, JSONException {
        ReadFromFile rff = new ReadFromFile();
        String jsonString = rff.readFromFile("CourseList.json");

        if (jsonString != null) {
            return JSONCourseListParser.parseJson(jsonString);
        }
        return null;
    }

    /**
     * Find currently selected course.
     * @return Course Current course if available, otherwise null.
     */
    public static Course getCurrentCourse() {
        Settings settings = PluginSettings.getSettings();

        CourseCollection courses;
        try {
            courses = CourseAndExerciseInfo.getCourses();
        } catch (Exception e) {
            return null;
        }

        if (courses != null) {
            return courses.searchCourse(settings.getSelectedCourse());

        }
        return null;
    }

    /**
     * Find exercises from currently selected course.
     * @return ExerciseCollection Current exercise list if available, otherwise null.
     */
    public static ExerciseCollection getCurrentExerciseList() {
        Course currentCourse = getCurrentCourse();
        if (currentCourse == null) {
            return null;
        }

        try {
            return getExercises(currentCourse);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Used to get an ExerciseCollection of current course from local JSON file.
     * @param course Current course
     * @return ExerciseCollection Exercises of current course or null if there is no JSON file available at the default folder.
     * @throws IOException
     * @throws JSONException
     */
    public static ExerciseCollection getExercises(Course course) throws IOException, JSONException {
        if (course == null) {
            throw new NullPointerException("course was null at CourseAndExerciseInfo.getExercises");
        }

        ReadFromFile rff = new ReadFromFile();
        String jsonString = rff.readFromFile(course.getName() + ".json");

        if (jsonString != null) {

            ExerciseCollection exerciseCollection = JSONExerciseListParser.parseJson(jsonString, course);

            return exerciseCollection;

        }
        return null;
    }
}
