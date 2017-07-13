package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ExerciseKey;
import fi.helsinki.cs.tmc.core.domain.Skill;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.persistance.ConfigFileIo;
import fi.helsinki.cs.tmc.data.CourseListUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Stores the list of available courses, the current course and its exercise list.
 */
public class CourseDb {

    public static class ChangedEvent implements TmcEvent {}

    public static final Logger logger = Logger.getLogger(CourseDb.class.getName());
    private static CourseDb defaultInstance;

    public static synchronized CourseDb getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new CourseDb();
        }
        return defaultInstance;
    }

    private TmcEventBus eventBus;
    private ConfigFileIo configFile;
    private List<Course> availableCourses;
    private String currentCourseName;
    private Map<ExerciseKey, String> downloadedExerciseChecksums;

    private CourseDb() {
        this(TmcEventBus.getDefault(), new ConfigFileIo("CourseDb.json"));
    }

    public CourseDb(TmcEventBus eventBus, ConfigFileIo configFile) {
        this.eventBus = eventBus;
        this.configFile = configFile;
        this.availableCourses = new ArrayList<>();
        this.currentCourseName = null;
        this.downloadedExerciseChecksums = new HashMap<>();
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
        this.currentCourseName = currentCourseName;
        save();
    }

    public int getCurrentCourseWeekCount() {
        return getCurrentCourse().getWeekCount();
    }

    public List<Exercise> getExercisesByWeek(int week) {
        return getCurrentCourse().getExercisesByWeek(week);
    }
    
    public List<Exercise> getStandardExercisesByWeek(int week) {
        List<Exercise> standardExercises = new ArrayList<>();
        for (Exercise ex : this.getExercisesByWeek(week)) {
            if (!ex.isAdaptive()) {
                standardExercises.add(ex);
            }
        }
        
        return standardExercises;
    }

    public int getPresumedCurrentWeek() {
        Project project = OpenProjects.getDefault().getOpenProjects()[0];
        ProjectMediator projectMediator = ProjectMediator.getInstance();
        Exercise exercise = projectMediator
            .tryGetExerciseForProject(projectMediator.wrapProject(project),
                CourseDb.getInstance());
        return exercise.getWeek();
    }

    public int getCurrentWeek() {
        for (int i = getCurrentCourseWeekCount(); i > 0; i--) {
            for (Exercise ex : getExercisesByWeek(i)) {
                if (ex.isAttempted()) {
                    return i;
                }
            }
        }
        return 1; //default
    }

    public int getAdaptiveWeek() {
        int week = -1;
        for (int i = 1; i <= getCurrentCourseWeekCount(); i++) {
            boolean wholeWeekDone = true;
            for (Exercise ex : getExercisesByWeek(i)) {
                if (!ex.isCompleted() && !ex.isAdaptive()) {
                    wholeWeekDone = false;
                    break;
                }
            }
            if (wholeWeekDone) {
                week = i;
            }
        }
        return week;
    }

    public List<Skill> getSkillsByWeek(int week) {
        return getCurrentCourse().getSkillsByWeek(week);
    }

    public void putDetailedCourse(Course course) {
        for (int i = 0; i < availableCourses.size(); ++i) {
            if (availableCourses.get(i).getName().equals(course.getName())) {
                availableCourses.set(i, course);
                save();
                break;
            }
        }
    }

    public Exercise getExerciseByKey(ExerciseKey key) {
        for (Exercise ex : getCurrentCourseExercises()) {
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
            return Collections.emptyList();
        }
    }

    public Course getCourseByName(String name) {
        for (Course course : availableCourses) {
            if (course.getName().equals(name)) {
                return course;
            }
        }
        return null;
    }

    public boolean isUnlockable(Exercise ex) {
        Course course = getCourseByName(ex.getCourseName());
        return course != null && course.getUnlockables().contains(ex.getName());
    }

    /**
     * Returns all exercises from the current course that can be unlocked (and must be unlocked together).
     */
    public List<Exercise> getCurrentCourseUnlockableExercises() {
        List<Exercise> result = new ArrayList<Exercise>();
        Course course = getCurrentCourse();
        if (course != null) {
            List<String> unlockables = course.getUnlockables();
            if (unlockables == null) {
                unlockables = Collections.emptyList();
            }
            for (String exerciseName : unlockables) {
                for (Exercise ex : course.getExercises()) {
                    if (ex.getName().equals(exerciseName)) {
                        result.add(ex);
                    }
                }
            }
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
        eventBus.post(new ChangedEvent());
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
            if (stuff.availableCourses != null) {
                this.availableCourses.clear();
                this.availableCourses.addAll(stuff.availableCourses);
            }

            this.currentCourseName = stuff.currentCourseName;

            if (stuff.downloadedExerciseChecksums != null) {
                this.downloadedExerciseChecksums.clear();
                this.downloadedExerciseChecksums.putAll(stuff.downloadedExerciseChecksums);
            }
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
