package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.ExerciseCollection;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import org.junit.After;
import java.util.prefs.Preferences;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.testing.MockBgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.http.NetworkTasks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openide.util.NbPreferences;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TmcServerAccessTest {
    
    @Mock private NetworkTasks networkTasks;
    @Mock private CancellableCallable<String> mockDownload;
    
    private Preferences prefs;
    private ServerAccess serverAccess;
    
    @Before
    public void setUp() {
        prefs = NbPreferences.forModule(ServerAccess.class);
        
        MockitoAnnotations.initMocks(this);
        
        serverAccess = newServer();
        serverAccess.setBaseUrl("http://example.com");
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        prefs.removeNode();
    }
    
    private ServerAccess newServer() {
        return new ServerAccess(networkTasks);
    }
    
    private void nextDownloadReturns(String s) {
        try {
            when(mockDownload.call()).thenReturn(s);
        } catch (Exception e) {
            fail("should never happen");
        }
    }
    
    @Test
    public void itCanDownloadACourseListFromARemoteJSONFile() throws IOException {
        String exerciseUrl = "http://example.com/courses/123/exercises.json";
        when(networkTasks.downloadTextFile("http://example.com/courses.json")).thenReturn(mockDownload);
        nextDownloadReturns("[{name: \"MyCourse\", exercises_json: \"" + exerciseUrl + "\"}]");
        
        MockBgTaskListener<CourseCollection> listener = new MockBgTaskListener<CourseCollection>();
        serverAccess.startDownloadingCourseList(listener);
        
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
        when(networkTasks.downloadTextFile("http://example.com/courses.json")).thenReturn(mockDownload);
        nextDownloadReturns(
                "[{" +
                "name: \"MyExercise\"," +
                "return_address: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: null," +
                "publish_date: null," +
                "zip_url: \"http://example.com/courses/123/exercises/1.zip\"" +
                "}]");
        
        MockBgTaskListener<ExerciseCollection> listener = new MockBgTaskListener<ExerciseCollection>();
        serverAccess.startDownloadingExerciseList(course, listener);
        
        listener.waitForCall();
        listener.assertGotSuccess();
        
        Exercise ex = listener.result.getExerciseByName("MyExercise");
        assertNotNull(ex);
        assertEquals("http://example.com/courses/123/exercises/1/submissions", ex.getReturnAddress());
        assertEquals("http://example.com/courses/123/exercises/1.zip", ex.getDownloadAddress());
    }
    
    @Test
    public void itStoresTheBaseUrlInPreferences() {
        String url = "http://another.example.com";
        serverAccess.setBaseUrl(url);
        assertEquals(url, newServer().getBaseUrl());
    }
    
    @Test
    public void itStoresTheUsernameInPreferences() {
        String name = "JohnShepard";
        serverAccess.setUsername(name);
        assertEquals(name, newServer().getUsername());
    }
    
    @Test
    public void itStripsTrailingSlashesOffTheBaseUrl() {
        serverAccess.setBaseUrl("http://example.com");
        assertEquals("http://example.com", serverAccess.getBaseUrl());
        
        serverAccess.setBaseUrl("http://example.com/");
        assertEquals("http://example.com", serverAccess.getBaseUrl());
        
        serverAccess.setBaseUrl("http://example.com///////");
        assertEquals("http://example.com", serverAccess.getBaseUrl());
        
        serverAccess.setBaseUrl("http://example.com///////");
        assertEquals("http://example.com", newServer().getBaseUrl());
    }
}
