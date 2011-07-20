package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.server.TmcServerAccess;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LocalCourseCacheTest {
    
    @Mock private TmcServerAccess server;
    private fi.helsinki.cs.tmc.data.LocalCourseCache courseCache;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.courseCache = new LocalCourseCache();
    }
    
    @Test
    public void itShouldRefreshTheCourseListInTheBackground() {
        //TODO
    }
}
