package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.PreferencesUI;
import java.awt.event.ActionListener;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import java.awt.event.ActionEvent;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openide.DialogDescriptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShowSettingsActionTest {
    
    @Mock private TmcServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private ProjectMediator projectMediator;
    @Mock private PreferencesUIFactory prefUiFactory;
    @Mock private PreferencesUI prefUi;
    
    @Mock private SaveSettingsAction saveAction;
    @Captor private ArgumentCaptor<ActionListener> prefListenerCaptor;
    @Captor private ArgumentCaptor<ActionEvent> eventCaptor;
    
    private ShowSettingsAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(localCourseCache.getAvailableCourses()).thenReturn(new CourseCollection());
        when(prefUiFactory.createCurrentPreferencesUI()).thenReturn(prefUi);
        doNothing().when(prefUiFactory).showPreferencesDialog(prefListenerCaptor.capture());
        
        action = new ShowSettingsAction(prefUiFactory, saveAction, serverAccess, localCourseCache, projectMediator);
    }
    
    @Test
    public void itShouldShowThePreferencesDialog() {
        action.actionPerformed(null);
        verify(prefUiFactory).showPreferencesDialog(any(ActionListener.class));
    }
    
    @Test
    public void itShouldSetTheUsernameInThePreferencesPanel() {
        when(serverAccess.getUsername()).thenReturn("JohnShepard");
        action.actionPerformed(null);
        verify(prefUi).setUsername("JohnShepard");
    }
    
    @Test
    public void itShouldSetTheServerBaseUrlInThePreferencesPanel() {
        when(serverAccess.getBaseUrl()).thenReturn("http://www.example.com");
        action.actionPerformed(null);
        verify(prefUi).setServerBaseUrl("http://www.example.com");
    }
    
    @Test
    public void itShouldSetTheDefaultProjectDirectoryInThePreferencesPanel() {
        when(projectMediator.getProjectDir()).thenReturn("/foo/bar");
        action.actionPerformed(null);
        verify(prefUi).setProjectDir("/foo/bar");
    }
    
    @Test
    public void itShouldSetTheCachedCourseListInThePreferencesPanel() {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        when(localCourseCache.getAvailableCourses()).thenReturn(courses);
        
        action.actionPerformed(null);
        
        verify(prefUi).setAvailableCourses(courses);
    }
    
    @Test
    public void itShouldSetSelectedCourseInThePreferencesPanel() {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        when(localCourseCache.getAvailableCourses()).thenReturn(courses);
        when(localCourseCache.getCurrentCourse()).thenReturn(courses.get(1));
        
        action.actionPerformed(null);
        
        verify(prefUi).setSelectedCourse(courses.get(1));
    }

    @Test
    public void itShouldCallTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        prefListenerCaptor.getValue().actionPerformed(
                new ActionEvent(DialogDescriptor.OK_OPTION, 0, null)
                );
        
        verify(saveAction).actionPerformed(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertSame(prefUi, eventCaptor.getValue().getSource());
    }
    
    @Test
    public void itShouldNotCallTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        prefListenerCaptor.getValue().actionPerformed(
                new ActionEvent(DialogDescriptor.CANCEL_OPTION, 0, null)
                );
        
        verifyZeroInteractions(saveAction);
    }
}
