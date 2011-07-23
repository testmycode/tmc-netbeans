package fi.helsinki.cs.tmc.actions;

import java.io.IOException;
import fi.helsinki.cs.tmc.data.Course;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class RefreshCoursesActionTest {
    
    @Mock private TmcServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private PreferencesUIFactory prefUiFactory;
    @Mock private PreferencesUI prefUi;
    @Mock private ModalDialogDisplayer dialogs;
    
    private CourseCollection courses;
    
    @Captor private ArgumentCaptor<BgTaskListener<CourseCollection>> downloadListenerCaptor;
    
    private RefreshCoursesAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        this.action = new RefreshCoursesAction(serverAccess, localCourseCache, prefUiFactory, dialogs);
    }
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    private void respondWithThreeCourses() {
        courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        getDownloadListener().backgroundTaskReady(courses);
    }
    
    private BgTaskListener<CourseCollection> getDownloadListener() {
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
        dialogs.displayError("Course refresh failed.\nWhoops");
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
}
