package fi.helsinki.cs.tmc.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.Refactored;
import fi.helsinki.cs.tmc.settings.ConfigFile;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The local course and exercise cache.
 */
@Refactored
public class LocalCourseCache {
    
    public static final Logger logger = Logger.getLogger(LocalCourseCache.class.getName());
    private static LocalCourseCache defaultInstance;
    
    public static LocalCourseCache getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new LocalCourseCache();
        }
        return defaultInstance;
    }

    private ConfigFile configFile;
    private CourseCollection availableCourses;
    private Course currentCourse;
    private ExerciseCollection availableExercises; // (for the current course)

    public LocalCourseCache() {
        this(new ConfigFile("LocalCourseCache.json"));
    }
    
    public LocalCourseCache(ConfigFile configFile) {
        this.configFile = configFile;
        this.availableCourses = new CourseCollection();
        this.availableExercises = null;
        try {
            loadFromFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load local course cache", e);
        }
    }
    
    public CourseCollection getAvailableCourses() {
        return availableCourses;
    }

    public void setAvailableCourses(CourseCollection availableCourses) {
        this.availableCourses = availableCourses;
        trySaveToFile();
    }
    
    public boolean hasCurrentCourse() {
        return availableExercises != null;
    }

    public Course getCurrentCourse() {
        return currentCourse;
    }

    public void setCurrentCourse(Course currentCourse) {
        this.currentCourse = currentCourse;
        trySaveToFile();
    }

    /**
     * Find exercises from currently selected course, or null if no current course.
     */
    public ExerciseCollection getAvailableExercises() {
        return availableExercises;
    }

    public void setAvailableExercises(ExerciseCollection availableExercises) {
        this.availableExercises = availableExercises;
        trySaveToFile();
    }

    @Deprecated
    public ExerciseCollection getExercisesForCourse(Course course) {
        //TODO
        return null;
    }

    private void loadFromFile() throws IOException {
        Gson gson = new Gson();
        Reader reader = configFile.getReader();
        StoredStuff stuff;
        try {
            stuff = gson.fromJson(reader, StoredStuff.class);
        } finally {
            reader.close();
        }
        if (stuff != null) {
            this.availableCourses = new CourseCollection();
            if (stuff.availableCourses != null) {
                this.availableCourses.addAll(stuff.availableCourses);
            }
            
            this.currentCourse = stuff.currentCourse;
            
            this.availableExercises = new ExerciseCollection();
            if (stuff.availableExercises != null) {
                this.availableExercises.addAll(stuff.availableExercises);
            }
        }
    }
    
    private void trySaveToFile() {
        try {
            saveToFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to save local course cache", e);
        }
    }
    
    private void saveToFile() throws IOException {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        StoredStuff stuff = new StoredStuff();
        stuff.availableCourses = this.availableCourses;
        stuff.currentCourse = this.getCurrentCourse();
        stuff.availableExercises = this.availableExercises;
        Writer w = configFile.getWriter();
        try {
            gson.toJson(stuff, w);
        } finally {
            w.close();
        }
    }
    
    private static class StoredStuff {
        public ArrayList<Course> availableCourses;
        public Course currentCourse;
        public ArrayList<Exercise> availableExercises;
    }
}
