package fi.helsinki.cs.tmc.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores list of available courses, the current course and its exercise list.
 */
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
    private String currentCourseName;

    public LocalCourseCache() {
        this(new ConfigFile("LocalCourseCache.json"));
    }
    
    public LocalCourseCache(ConfigFile configFile) {
        this.configFile = configFile;
        this.availableCourses = new CourseCollection();
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
        save();
    }

    public Course getCurrentCourse() {
        return availableCourses.getCourseByName(currentCourseName);
    }

    public String getCurrentCourseName() {
        return currentCourseName;
    }

    public void setCurrentCourseName(String currentCourseName) {
        if (availableCourses.hasCourseByName(currentCourseName)) {
            this.currentCourseName = currentCourseName;
            save();
        } else {
            logger.warning("Tried to set current course set to one not in available courses");
        }
    }

    /**
     * Returns the exercises from currently selected course.
     * 
     * <p>
     * If no course is currently selected then returns the empty collection.
     */
    public ExerciseCollection getCurrentCourseExercises() {
        Course course = getCurrentCourse();
        if (course != null) {
            return course.getExercises();
        } else {
            return new ExerciseCollection();
        }
    }
    
    /**
     * Returns all exercises from all courses.
     */
    public ExerciseCollection getAllExercises() {
        ExerciseCollection result = new ExerciseCollection();
        for (Course course : availableCourses) {
            result.addAll(course.getExercises());
        }
        return result;
    }
    
    public void save() {
        try {
            saveToFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to save local course cache", e);
        }
    }
    
    private static class StoredStuff {
        public ArrayList<Course> availableCourses;
        public String currentCourseName;
    }
    
    private void saveToFile() throws IOException {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        StoredStuff stuff = new StoredStuff();
        stuff.availableCourses = this.availableCourses;
        stuff.currentCourseName = this.currentCourseName;
        Writer w = configFile.getWriter();
        try {
            gson.toJson(stuff, w);
        } finally {
            w.close();
        }
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
            
            this.currentCourseName = stuff.currentCourseName;
        }
    }
}
