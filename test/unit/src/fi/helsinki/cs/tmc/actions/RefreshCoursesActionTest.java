package fi.helsinki.cs.tmc.actions;

import java.io.IOException;
import fi.helsinki.cs.tmc.data.Course;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class RefreshCoursesActionTest {
    
    @Mock private ServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private PreferencesUIFactory prefUiFactory;
    @Mock private PreferencesUI prefUi;
    @Mock private ConvenientDialogDisplayer dialogs;
    
    private CourseList courses;
    
    @Captor private ArgumentCaptor<BgTaskListener<CourseList>> downloadListenerCaptor;
    
    private RefreshCoursesAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(serverAccess.getBaseUrl()).thenReturn("http://default.example.com");
        
        this.action = new RefreshCoursesAction(serverAccess, localCourseCache, prefUiFactory, dialogs);
    }
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    private void respondWithThreeCourses() {
        courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        getDownloadListener().backgroundTaskReady(courses);
    }
    
    private BgTaskListener<CourseList> getDownloadListener() {
        verify(serverAccess).startDownloadingCourseList(downloadListenerCaptor.capture());
        return downloadListenerCaptor.getValue();
    }
    
    @Test
    public void whenDownloadSucceedsItShouldRefreshTheCourseListInTheLocalCache() {
        performAction();
        respondWithThreeCourses();
        
        verify(localCourseCache).setAvailableCourses(courses);
    }
    
    @Test
    public void whenDownloadSucceedsAndThereIsAPreferencesUIItShouldUpdateTheCourseListInTheUI() {
        when(prefUiFactory.getCurrentUI()).thenReturn(prefUi);
        performAction();
        respondWithThreeCourses();
        
        verify(prefUi).setAvailableCourses(courses);
    }
    
    @Test
    public void whenDownloadSucceedsAndThereIsNoPreferencesUIItShouldNotTryToUpdateTheCourseListInTheUI() {
        when(prefUiFactory.getCurrentUI()).thenReturn(null);
        performAction();
        respondWithThreeCourses();
        
        verifyZeroInteractions(prefUi);
    }
    
    @Test
    public void whenDownloadIsCancelledItShouldNotChangeTheCourseCache() {
        performAction();
        getDownloadListener().backgroundTaskCancelled();
        verifyZeroInteractions(localCourseCache);
    }
    
    @Test
    public void whenDownloadIsCancelledItShouldInformThePreferencesUiIfActive() {
        when(prefUiFactory.getCurrentUI()).thenReturn(prefUi);
        performAction();
        getDownloadListener().backgroundTaskFailed(new IOException("Whoops"));
        verify(prefUi).courseRefreshFailedOrCanceled();
    }
    
    @Test
    public void whenDownloadFailsItShouldDisplayAnError() {
        performAction();
        getDownloadListener().backgroundTaskFailed(new IOException("Whoops"));
        verify(dialogs).displayError("Course refresh failed.\nWhoops");
    }
    
    @Test
    public void whenDownloadFailsItShouldInformThePreferencesUiIfActive() {
        when(prefUiFactory.getCurrentUI()).thenReturn(prefUi);
        performAction();
        getDownloadListener().backgroundTaskFailed(new IOException("Whoops"));
        verify(prefUi).courseRefreshFailedOrCanceled();
    }
    
    @Test
    public void whenThePreferencesUIIsVisibleItShouldUseTheBaseUrlInTheEditor() {
        when(prefUiFactory.getCurrentUI()).thenReturn(prefUi);
        when(prefUi.getServerBaseUrl()).thenReturn("http://another.example.com");
        performAction();
        verify(serverAccess).setBaseUrl("http://another.example.com");
    }
    
    @Test
    public void whenTheBaseUrlIsNullItShouldDisplayAHelpfulError() {
        when(serverAccess.getBaseUrl()).thenReturn(null);
        performAction();
        verify(dialogs).displayError("Please set the server address first.");
        verify(serverAccess, never()).startDownloadingCourseList(any(BgTaskListener.class));
    }
    
    @Test
    public void whenTheBaseUrlIsEmptyItShouldDisplayAHelpfulError() {
        when(serverAccess.getBaseUrl()).thenReturn("   ");
        performAction();
        verify(dialogs).displayError("Please set the server address first.");
        verify(serverAccess, never()).startDownloadingCourseList(any(BgTaskListener.class));
    }
    
    @Test
    public void whenTheBaseUrlIsNullOrEmptyItShouldInformThePreferencesUiIfActive() {
        when(prefUiFactory.getCurrentUI()).thenReturn(prefUi);
        
        when(serverAccess.getBaseUrl()).thenReturn(null);
        performAction();
        when(serverAccess.getBaseUrl()).thenReturn("   ");
        performAction();
        
        verify(prefUi, times(2)).courseRefreshFailedOrCanceled();
    }
}
