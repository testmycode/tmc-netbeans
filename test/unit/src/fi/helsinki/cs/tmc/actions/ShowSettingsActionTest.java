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
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    
    @Test
    public void itShouldShowThePreferencesDialog() {
        performAction();
        verify(prefUiFactory).showPreferencesDialog(any(ActionListener.class));
    }
    
    @Test
    public void whenThePreferencesDialogIsAlreadyVisibleItShouldActivateTheDialogIt() {
        when(prefUiFactory.isPreferencesUiVisible()).thenReturn(true);
        performAction();
        verify(prefUiFactory).activateVisiblePreferencesUi();
        verify(prefUiFactory, never()).createCurrentPreferencesUI();
    }
    
    @Test
    public void itShouldSetTheUsernameInThePreferencesPanel() {
        when(serverAccess.getUsername()).thenReturn("JohnShepard");
        performAction();
        verify(prefUi).setUsername("JohnShepard");
    }
    
    @Test
    public void itShouldSetTheServerBaseUrlInThePreferencesPanel() {
        when(serverAccess.getBaseUrl()).thenReturn("http://www.example.com");
        performAction();
        verify(prefUi).setServerBaseUrl("http://www.example.com");
    }
    
    @Test
    public void itShouldSetTheDefaultProjectDirectoryInThePreferencesPanel() {
        when(projectMediator.getProjectDir()).thenReturn("/foo/bar");
        performAction();
        verify(prefUi).setProjectDir("/foo/bar");
    }
    
    @Test
    public void itShouldSetTheCachedCourseListInThePreferencesPanel() {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        when(localCourseCache.getAvailableCourses()).thenReturn(courses);
        
        performAction();
        
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
        
        performAction();
        
        verify(prefUi).setSelectedCourse(courses.get(1));
    }

    @Test
    public void itShouldCallTheSaveSettingsActionIfTheUserPressesOK() {
        performAction();
        prefListenerCaptor.getValue().actionPerformed(
                new ActionEvent(DialogDescriptor.OK_OPTION, 0, null)
                );
        
        verify(saveAction).actionPerformed(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertSame(prefUi, eventCaptor.getValue().getSource());
    }
    
    @Test
    public void itShouldNotCallTheSaveSettingsActionIfTheUserPressesOK() {
        performAction();
        prefListenerCaptor.getValue().actionPerformed(
                new ActionEvent(DialogDescriptor.CANCEL_OPTION, 0, null)
                );
        
        verifyZeroInteractions(saveAction);
    }
}
