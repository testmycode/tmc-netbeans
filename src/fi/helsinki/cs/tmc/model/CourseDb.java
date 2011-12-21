package fi.helsinki.cs.tmc.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseKey;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores the list of available courses, the current course and its exercise list.
 */
public class CourseDb {

    public static class SavedEvent implements TmcEvent {}
    
    public static final Logger logger = Logger.getLogger(CourseDb.class.getName());
    private static CourseDb defaultInstance;
    
    public static CourseDb getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new CourseDb();
        }
        return defaultInstance;
    }

    private TmcEventBus eventBus;
    private ConfigFile configFile;
    private List<Course> availableCourses;
    private String currentCourseName;
    private Map<ExerciseKey, String> downloadedExerciseChecksums;

    public CourseDb() {
        this(TmcEventBus.getInstance(), new ConfigFile("CourseDb.json"));
    }
    
    public CourseDb(TmcEventBus eventBus, ConfigFile configFile) {
        this.eventBus = eventBus;
        this.configFile = configFile;
        this.availableCourses = new ArrayList<Course>();
        this.currentCourseName = null;
        this.downloadedExerciseChecksums = new HashMap<ExerciseKey, String>();
        try {
            loadFromFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load course database", e);
        }
    }
    
    public List<Course> getAvailableCourses() {
        return Collections.unmodifiableList(availableCourses);
    }

    public void setAvailableCourses(List<Course> availableCourses) {
        this.availableCourses = availableCourses;
        save();
    }

    public Course getCurrentCourse() {
        return CourseListUtils.getCourseByName(availableCourses, currentCourseName);
    }

    public String getCurrentCourseName() {
        return currentCourseName;
    }

    public void setCurrentCourseName(String currentCourseName) {
        if (CourseListUtils.hasCourseByName(availableCourses, currentCourseName)) {
            this.currentCourseName = currentCourseName;
            save();
        } else {
            logger.warning("Tried to set current course to one not in available courses");
        }
    }

    public Exercise getExerciseByKey(ExerciseKey key) {
        for (Exercise ex : getAllExercises()) {
            if (key.equals(ex.getKey())) {
                return ex;
            }
        }
        return null;
    }

    /**
     * Returns the exercises from currently selected course.
     * 
     * <p>
     * If no course is currently selected then returns the empty collection.
     */
    public List<Exercise> getCurrentCourseExercises() {
        Course course = getCurrentCourse();
        if (course != null) {
            return course.getExercises();
        } else {
            return new ArrayList<Exercise>();
        }
    }
    
    /**
     * Returns all exercises from all courses.
     */
    public List<Exercise> getAllExercises() {
        List<Exercise> result = new ArrayList<Exercise>();
        for (Course course : availableCourses) {
            result.addAll(course.getExercises());
        }
        return result;
    }

    public String getDownloadedExerciseChecksum(ExerciseKey ex) {
        return downloadedExerciseChecksums.get(ex);
    }
    
    /**
     * Informs the course database that the exercise is considered downloaded.
     * 
     * <p>
     * Sets the downloaded checksum of the exercise to be the one reported by the server.
     */
    public void exerciseDownloaded(Exercise ex) {
        downloadedExerciseChecksums.put(ex.getKey(), ex.getChecksum());
        save();
    }
    
    //TODO: arrange for downloadedExerciseChecksums.put(..., null) when a project is deleted!
    
    public void save() {
        try {
            saveToFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to save course database", e);
        }
        eventBus.post(new SavedEvent());
    }
    
    private static class StoredStuff {
        public List<Course> availableCourses;
        public String currentCourseName;
        public Map<ExerciseKey, String> downloadedExerciseChecksums;
    }
    
    private void saveToFile() throws IOException {
        StoredStuff stuff = new StoredStuff();
        stuff.availableCourses = this.availableCourses;
        stuff.currentCourseName = this.currentCourseName;
        stuff.downloadedExerciseChecksums = this.downloadedExerciseChecksums;
        Writer w = configFile.getWriter();
        try {
            getGson().toJson(stuff, w);
        } finally {
            w.close();
        }
    }

    private void loadFromFile() throws IOException {
        if (!configFile.exists()) {
            return;
        }
        
        Reader reader = configFile.getReader();
        StoredStuff stuff;
        try {
            stuff = getGson().fromJson(reader, StoredStuff.class);
        } finally {
            reader.close();
        }
        if (stuff != null) {
            this.availableCourses = new ArrayList<Course>();
            if (stuff.availableCourses != null) {
                this.availableCourses.addAll(stuff.availableCourses);
            }
            
            this.currentCourseName = stuff.currentCourseName;
            this.downloadedExerciseChecksums = stuff.downloadedExerciseChecksums;
        }
    }
    
    private Gson getGson() {
        return new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .registerTypeAdapter(ExerciseKey.class, new ExerciseKey.GsonAdapter())
                .create();
    }
}
