package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.File;
import java.util.ArrayList;
import com.google.gson.JsonObject;
import static fi.helsinki.cs.tmc.testing.JsonBuilder.*;

/**
 * Provides a default fixture for functional tests.
 */
public class FullServerFixture {
    private FakeTmcServer fakeServer;
    
    public static class CourseFixture implements Jsonable {
        public String name;
        public ArrayList<ExerciseFixture> exercises;

        public CourseFixture(String name) {
            this.name = name;
            this.exercises = new ArrayList<ExerciseFixture>();
        }
        
        @Override
        public JsonObject toJson() {
            return object(
                        prop("name", name),
                        prop("exercises", array(exercises.toArray()))
                    );
        }
    }
    
    public static class ExerciseFixture implements Jsonable {
        public String name;
        public String deadline = null;
        public String returnUrl = "http://example.com/bogus.json";
        public String zipUrl = "http://example.com/bogus.zip";
        public boolean returnable = true;
        public boolean attempted = false;
        public boolean completed = false;
        
        public File zipFile;

        public ExerciseFixture(String name) {
            this.name = name;
        }
        
        @Override
        public JsonObject toJson() {
            return object(
                        prop("name", name),
                        prop("deadline", deadline),
                        prop("return_url", returnUrl),
                        prop("zip_url", zipUrl),
                        prop("returnable", returnable),
                        prop("attempted", attempted),
                        prop("completed", completed)
                    );
        }
    }
    
    // Settings to change before calling setUp().
    public String expectedUser = "theuser";
    public String expectedPassword = "thepassword";
    
    private ArrayList<CourseFixture> courses = new ArrayList<CourseFixture>();
    
    public FullServerFixture() throws Exception {
        fakeServer = new FakeTmcServer();
        fakeServer.start();
    }
    
    public void addEmptyCourse(String name) {
        courses.add(new CourseFixture(name));
        updateServerCourseList();
    }
    
    public void addDefaultCourse(String name, File zipFile) {
        CourseFixture course = new CourseFixture(name);
        
        ExerciseFixture expiredEx = new ExerciseFixture("ExpiredExercise");
        expiredEx.deadline = "2010-01-01T23:59:59+02:00";
        course.exercises.add(expiredEx);
        
        ExerciseFixture testEx = new ExerciseFixture("TestExercise");
        testEx.returnUrl = fakeServer.getBaseUrl() + "/courses/123/exercises/456/submissions.json";
        testEx.zipUrl = fakeServer.getBaseUrl() + "/courses/123/exercises/456.zip";
        testEx.zipFile = zipFile;
        course.exercises.add(testEx);
        
        courses.add(course);
        updateServerCourseList();
    }
    
    private void updateServerCourseList() {
        JsonObject json =
                object(
                    prop("api_version", 1),
                    prop("courses", array(courses.toArray()))
                );
        fakeServer.respondWithCourses(json.toString());
        
        fakeServer.clearZipFiles();
        for (CourseFixture course : courses) {
            for (ExerciseFixture exercise : course.exercises) {
                if (exercise.zipFile != null && exercise.zipUrl.startsWith(fakeServer.getBaseUrl())) {
                    String zipPath = exercise.zipUrl.substring(fakeServer.getBaseUrl().length());
                    fakeServer.addZipFile(zipPath, exercise.zipFile);
                }
            }
        }
    }
    
    public Iterable<CourseFixture> getCourses() {
        return courses;
    }
    
    public void tearDown() throws Exception {
        if (fakeServer != null) {
            fakeServer.stop();
            fakeServer = null;
        }
    }
    
    public FakeTmcServer getFakeServer() {
        return fakeServer;
    }
}
