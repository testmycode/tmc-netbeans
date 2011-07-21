package fi.helsinki.cs.tmc.server;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import java.io.IOException;
import fi.helsinki.cs.tmc.testing.MockBgTaskListener;
import fi.helsinki.cs.tmc.utilities.http.FileDownloader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TmcServerAccessTest {
    
    @Mock private FileDownloader downloader;
    
    private TmcServerAccess server;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        server = new TmcServerAccess("http://example.com", downloader);
    }
    
    @Test
    public void itCanDownloadACourseListFromARemoteJSONFile() throws IOException {
        String exerciseUrl = "http://example.com/courses/123/exercises.json";
        MockBgTaskListener<CourseCollection> listener = new MockBgTaskListener<CourseCollection>();
        when(downloader.downloadTextFile("http://example.com/courses.json")).thenReturn(
                "[{name: \"MyCourse\", exercises_json: \"" + exerciseUrl + "\"}]"
                );
        
        server.startDownloadingCourseList(listener);
        
        listener.waitForCall();
        listener.assertGotSuccess();
        Course course = listener.result.getCourseByName("MyCourse");
        assertEquals("MyCourse", course.getName());
        assertEquals(exerciseUrl, course.getExerciseListDownloadAddress());
    }
    
    @Test
    public void itCanDownloadTheListOfExercisesForACourseFromARemoteJSONFile() throws IOException {
        String exerciseUrl = "http://example.com/courses/123/exercises.json";
        Course course = new Course();
        course.setExerciseListDownloadAddress(exerciseUrl);
        
        MockBgTaskListener<ExerciseCollection> listener = new MockBgTaskListener<ExerciseCollection>();
        when(downloader.downloadTextFile(exerciseUrl)).thenReturn(
                "[{" +
                "name: \"MyExercise\"," +
                "return_address: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: null," +
                "publish_date: null," +
                "exercise_file: \"http://example.com/courses/123/exercises/1.zip\"" +
                "}]"
                );
        
        server.startDownloadingExerciseList(course, listener);
        
        listener.waitForCall();
        listener.assertGotSuccess();
        
        Exercise ex = listener.result.getExerciseByName("MyExercise");
        assertNotNull(ex);
        assertEquals("http://example.com/courses/123/exercises/1/submissions", ex.getReturnAddress());
        assertEquals("http://example.com/courses/123/exercises/1.zip", ex.getDownloadAddress());
    }
}
