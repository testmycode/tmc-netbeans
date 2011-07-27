package fi.helsinki.cs.tmc.model;

import org.mockito.ArgumentMatcher;
import java.util.Map;
import java.io.File;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectZipper;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import org.junit.After;
import java.util.prefs.Preferences;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.SubmissionResult;
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

public class ServerAccessTest {
    
    @Mock private NetworkTasks networkTasks;
    @Mock private ProjectMediator projectMediator;
    @Mock private NbProjectUnzipper unzipper;
    @Mock private NbProjectZipper zipper;
    @Mock private CancellableCallable<String> mockTextDownload;
    @Mock private CancellableCallable<byte[]> mockBinaryDownload;
    
    private byte[] fakeBinaryData;
    private Preferences prefs;
    
    private ServerAccess serverAccess;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fakeBinaryData = new byte[] {1, 2, 3, 4, 5};
        prefs = NbPreferences.forModule(ServerAccess.class);
        
        serverAccess = newServer();
        serverAccess.setBaseUrl("http://example.com");
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        prefs.removeNode();
    }

    private ServerAccess newServer() {
        return new ServerAccess(networkTasks, projectMediator, unzipper, zipper);
    }
    
    private void nextTextDownloadReturns(String s) {
        try {
            when(mockTextDownload.call()).thenReturn(s);
        } catch (Exception e) {
            fail("should never happen");
        }
    }
    
    private void nextBinaryDownloadReturns(byte[] data) {
        try {
            when(mockBinaryDownload.call()).thenReturn(data);
        } catch (Exception e) {
            fail("should never happen");
        }
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
    
    
    @Test
    public void itCanDownloadACourseListFromARemoteJSONFile() throws IOException {
        String exerciseUrl = "http://example.com/courses/123/exercises.json";
        when(networkTasks.downloadTextFile("http://example.com/courses.json")).thenReturn(mockTextDownload);
        nextTextDownloadReturns("[{name: \"MyCourse\", exercises_json: \"" + exerciseUrl + "\"}]");
        
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
        Course course = new Course("MyCourse");
        course.setExerciseListDownloadAddress(exerciseUrl);
        when(networkTasks.downloadTextFile(exerciseUrl)).thenReturn(mockTextDownload);
        nextTextDownloadReturns(
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
        assertEquals("MyCourse", ex.getCourseName());
    }
    
    
    @Test
    public void itCanDownloadAnExerciseFromTheServerAndUnzipItAsAProject() throws IOException {
        Exercise exercise = makeDownloadableExercise("Dir1-Dir2-MyEx");
        
        TmcProjectInfo project = mock(TmcProjectInfo.class);
        when(projectMediator.getCourseRootDir("MyCourse")).thenReturn(new File("/foo/MyCourse"));
        when(projectMediator.tryGetProjectForExercise(exercise)).thenReturn(project);
        
        MockBgTaskListener<TmcProjectInfo> listener = new MockBgTaskListener<TmcProjectInfo>();
        serverAccess.startDownloadingExerciseProject(exercise, listener);
        
        listener.waitForCall();
        listener.assertGotSuccess();
        
        assertSame(project, listener.result);
        verify(unzipper).unzipProject(fakeBinaryData, new File("/foo/MyCourse"), "Dir1-Dir2-MyEx");
    }
    
    @Test
    public void whenNetBeansCannotOpenADownloadedExerciseItShouldThrowAnExceptionInTheDownloadThread() throws IOException {
        Exercise exercise = makeDownloadableExercise("Dir1-Dir2-MyEx");
        
        when(projectMediator.getCourseRootDir("MyCourse")).thenReturn(new File("/foo/MyCourse"));
        when(projectMediator.tryGetProjectForExercise(exercise)).thenReturn(null);
        
        MockBgTaskListener<TmcProjectInfo> listener = new MockBgTaskListener<TmcProjectInfo>();
        serverAccess.startDownloadingExerciseProject(exercise, listener);
        
        listener.waitForCall();
        
        assertNotNull(listener.taskException);
        verify(unzipper).unzipProject(fakeBinaryData, new File("/foo/MyCourse"), "Dir1-Dir2-MyEx");
    }
    
    private Exercise makeDownloadableExercise(String exerciseName) {
        String zipUrl = "http://example.com/courses/123/exercises/456.zip";
        Exercise exercise = new Exercise(exerciseName);
        exercise.setCourseName("MyCourse");
        exercise.setDownloadAddress(zipUrl);
        when(networkTasks.downloadBinaryFile(zipUrl)).thenReturn(mockBinaryDownload);
        nextBinaryDownloadReturns(fakeBinaryData);
        return exercise;
    }
    
    
    @Test
    public void itCanSubmitAndExerciseToTheServer() throws IOException {
        String submitUrl = "http://example.com/courses/123/exercises/456/submissions";
        Exercise exercise = new Exercise("MyExercise");
        exercise.setReturnAddress(submitUrl);
        
        when(projectMediator.getProjectDirForExercise(exercise)).thenReturn(new File("/foo/MyExercise"));
        when(zipper.zipProjectSources("/foo/MyExercise")).thenReturn(fakeBinaryData);
        when(networkTasks.uploadFileForTextResponse(
                eq(submitUrl),
                (Map<String, String>)any(Map.class),
                any(String.class),
                any(byte[].class)
                )).thenReturn(mockTextDownload);
        nextTextDownloadReturns("{status: \"ok\"}");
        
        MockBgTaskListener<SubmissionResult> listener = new MockBgTaskListener<SubmissionResult>();
        serverAccess.setUsername("JohnShepard");
        serverAccess.startSubmittingExercise(exercise, listener);
        
        listener.waitForCall();
        assertTrue(listener.success);
        assertEquals(SubmissionResult.Status.OK, listener.result.getStatus());
        
        ArgumentMatcher<Map<String, String>> givesUsernameParam = new ArgumentMatcher<Map<String, String>>() {
            @Override
            public boolean matches(Object argument) {
                Map<String, String> map = (Map<String, String>)argument;
                String key = "submission[username]";
                return map.containsKey(key) && map.get(key).equals("JohnShepard");
            }
        };
        verify(networkTasks).uploadFileForTextResponse(eq(submitUrl), argThat(givesUsernameParam), eq("submission[file]"), eq(fakeBinaryData));
    }
}
