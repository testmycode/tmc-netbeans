package fi.helsinki.cs.tmc.functionaltests.utils;

import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.google.gson.JsonObject;
import static fi.helsinki.cs.tmc.testing.JsonBuilder.*;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Provides a default fixture for functional tests.
 */
public class FullServerFixture {
    private FakeTmcServer fakeServer;

    public class CourseSummaryFixture implements Jsonable {
        public String id;
        public String name;

        public CourseSummaryFixture(String id, String name) {
            this.id = id;
            this.name = name;
        }

        protected Prop[] props() {
            return new Prop[] {
                prop("id", id),
                prop("name", name),
                prop("details_url", fakeServer.getBaseUrl() + "/courses/" + id + ".json")
            };
        }

        @Override
        public JsonElement toJson() {
            return object(props());
        }
    }

    public class CourseFixture extends CourseSummaryFixture {
        public ArrayList<ExerciseFixture> exercises;

        public CourseFixture(String id, String name) {
            super(id, name);
            this.exercises = new ArrayList<ExerciseFixture>();
        }

        public ExerciseFixture getExerciseFixture(String name) {
            for (ExerciseFixture ex : exercises) {
                if (ex.name.equals(name)) {
                    return ex;
                }
            }
            return null;
        }

        @Override
        protected Prop[] props() {
            return ArrayUtils.addAll(super.props(),
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
        public String checksum = "initialchecksum";
        
        public byte[] zipData;

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
                        prop("completed", completed),
                        prop("checksum", checksum)
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
        courses.add(new CourseFixture(UUID.randomUUID().toString(), name));
        updateServerCourseList();
    }

    public CourseFixture addDefaultCourse(String name, String exerciseName, File projectDir) throws IOException {
        byte[] zipData = new RecursiveZipper(projectDir, RecursiveZipper.ZIP_ALL_THE_THINGS).zipProjectSources();
        return addDefaultCourse(name, exerciseName, zipData);
    }

    public CourseFixture addDefaultCourse(String name, String exerciseName, byte[] zipData) {
        CourseFixture course = new CourseFixture(UUID.randomUUID().toString(), name);
        
        ExerciseFixture expiredEx = new ExerciseFixture("ExpiredExercise");
        expiredEx.deadline = "2010-01-01T23:59:59+02:00";
        course.exercises.add(expiredEx);
        
        ExerciseFixture testEx = new ExerciseFixture(exerciseName);
        testEx.returnUrl = fakeServer.getBaseUrl() + "/courses/123/exercises/456/submissions.json";
        testEx.zipUrl = fakeServer.getBaseUrl() + "/courses/123/exercises/456.zip";
        testEx.zipData = zipData;
        course.exercises.add(testEx);
        
        courses.add(course);
        updateServerCourseList();
        
        return course;
    }
    
    public void updateServerCourseList() {
        fakeServer.clearResponses();

        fakeServer.respondWithCourses(object(
            prop("courses", array(getCourseSummaries().toArray()))
        ).toString());

        for (CourseFixture course : courses) {
            fakeServer.respondWithCourseDetails(course.id, object(
                prop("course", course)
            ).toString());
        }
        
        for (CourseFixture course : courses) {
            for (ExerciseFixture exercise : course.exercises) {
                if (exercise.zipData != null && exercise.zipUrl.startsWith(fakeServer.getBaseUrl())) {
                    String zipPath = exercise.zipUrl.substring(fakeServer.getBaseUrl().length());
                    fakeServer.putZipFile(zipPath, exercise.zipData);
                }
            }
        }
    }

    private List<CourseSummaryFixture> getCourseSummaries() {
        List<CourseSummaryFixture> summaries = new ArrayList<CourseSummaryFixture>();
        for (CourseFixture cf : courses) {
            summaries.add(new CourseSummaryFixture(cf.id, cf.name));
        }
        return summaries;
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
