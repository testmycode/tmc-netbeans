package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.Refactored;
import java.io.IOException;
import fi.helsinki.cs.tmc.utilities.textio.ReadFromFile;
import fi.helsinki.cs.tmc.settings.PluginSettings;
import fi.helsinki.cs.tmc.settings.Settings;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;

/**
 * The local course and exercise cache.
 */
@Refactored
public class LocalCourseCache {
    
    private static LocalCourseCache defaultInstance = new LocalCourseCache();
    
    public static LocalCourseCache getInstance() {
        return defaultInstance;
    }
    
    /**
     * Used to get a CourseCollection of current courses from local JSON file.
     * @return CourseCollection with current courses or null if there is no JSON file available at the default folder.
     */
    public CourseCollection getCourses() throws IOException {
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
    public Course getCurrentCourse() {
        Settings settings = PluginSettings.getSettings();

        CourseCollection courses;
        try {
            courses = getCourses();
        } catch (Exception e) {
            return null;
        }

        if (courses != null) {
            return courses.getCourseByName(settings.getSelectedCourse());

        }
        return null;
    }

    /**
     * Find exercises from currently selected course.
     * @return ExerciseCollection Current exercise list if available, otherwise null.
     */
    public ExerciseCollection getCurrentExerciseList() {
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
     * @return ExerciseCollection Exercises of current course or null if no current course.
     */
    public ExerciseCollection getExercises(Course course) throws IOException {
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
