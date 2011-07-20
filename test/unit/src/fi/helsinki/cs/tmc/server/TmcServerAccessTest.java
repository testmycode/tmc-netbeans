package fi.helsinki.cs.tmc.server;

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
}
