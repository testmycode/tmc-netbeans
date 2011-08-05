package fi.helsinki.cs.tmc.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.ExerciseList;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores the list of available courses, the current course and its exercise list.
 */
public class CourseDb {
    
    public static final Logger logger = Logger.getLogger(CourseDb.class.getName());
    private static CourseDb defaultInstance;
    
    public static CourseDb getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new CourseDb();
        }
        return defaultInstance;
    }

    private ConfigFile configFile;
    private CourseList availableCourses;
    private String currentCourseName;
    
    private List<CourseDbListener> listeners;

    public CourseDb() {
        this(new ConfigFile("CourseDb.json"));
    }
    
    public CourseDb(ConfigFile configFile) {
        this.configFile = configFile;
        this.availableCourses = new CourseList();
        this.listeners = new ArrayList<CourseDbListener>();
        try {
            loadFromFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load course database", e);
        }
    }
    
    public CourseList getAvailableCourses() {
        return availableCourses;
    }

    public void setAvailableCourses(CourseList availableCourses) {
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
    public ExerciseList getCurrentCourseExercises() {
        Course course = getCurrentCourse();
        if (course != null) {
            return course.getExercises();
        } else {
            return new ExerciseList();
        }
    }
    
    /**
     * Returns all exercises from all courses.
     */
    public ExerciseList getAllExercises() {
        ExerciseList result = new ExerciseList();
        for (Course course : availableCourses) {
            result.addAll(course.getExercises());
        }
        return result;
    }
    
    public void save() {
        try {
            saveToFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to save course database", e);
        }
        fireCourseDbSaved();
    }

    public void addListener(CourseDbListener listener) {
        listeners.add(listener);
    }
    
    private void fireCourseDbSaved() {
        for (CourseDbListener listener : listeners) {
            listener.courseDbSaved();
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
            this.availableCourses = new CourseList();
            if (stuff.availableCourses != null) {
                this.availableCourses.addAll(stuff.availableCourses);
            }
            
            this.currentCourseName = stuff.currentCourseName;
        }
    }
}
