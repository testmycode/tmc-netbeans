package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import java.awt.event.ActionEvent;
import fi.helsinki.cs.tmc.ui.PreferencesPanel;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import java.awt.Dialog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShowSettingsActionTest {
    
    @Mock private TmcServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private ProjectMediator projectMediator;
    
    // This is a bit of a mess :/
    @Mock private DialogDisplayer displayer;
    @Mock private Dialog dialog;
    @Mock private SaveSettingsAction saveAction;
    @Captor private ArgumentCaptor<DialogDescriptor> descriptorCaptor;
    @Captor private ArgumentCaptor<ActionEvent> eventCaptor;
    
    private ShowSettingsAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(localCourseCache.getAvailableCourses()).thenReturn(new CourseCollection());
        when(displayer.createDialog(descriptorCaptor.capture())).thenReturn(dialog);
        
        action = new ShowSettingsAction(displayer, saveAction, serverAccess, localCourseCache, projectMediator);
    }
    
    @Test
    public void itShouldShowThePreferencesDialog() {
        action.actionPerformed(null);
        verify(dialog).setVisible(true);
    }
    
    @Test
    public void itShouldSetTheUsernameInThePreferencesPanel() {
        when(serverAccess.getUsername()).thenReturn("JohnShepard");
        action.actionPerformed(null);
        assertEquals("JohnShepard", getCapturedPrefPanel().getUsername());
    }
    
    @Test
    public void itShouldSetTheServerBaseUrlInThePreferencesPanel() {
        when(serverAccess.getBaseUrl()).thenReturn("http://www.example.com");
        action.actionPerformed(null);
        PreferencesPanel panel = getCapturedPrefPanel();
        assertEquals("http://www.example.com", panel.getServerBaseUrl());
    }
    
    @Test
    public void itShouldSetTheDefaultProjectDirectoryInThePreferencesPanel() {
        when(projectMediator.getProjectDir()).thenReturn("/foo/bar");
        action.actionPerformed(null);
        assertEquals("/foo/bar", getCapturedPrefPanel().getProjectDir());
    }
    
    @Test
    public void itShouldSetTheCachedCourseListInThePreferencesPanel() {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.add(new Course("three"));
        when(localCourseCache.getAvailableCourses()).thenReturn(courses);
        
        action.actionPerformed(null);
        
        assertEquals(3, getCapturedPrefPanel().getAvailableCourseCount());
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
        
        assertSame(courses.get(1), getCapturedPrefPanel().getSelectedCourse());
    }

    @Test
    public void itShouldCallTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        descriptorCaptor.getValue().getButtonListener().actionPerformed(
                new ActionEvent(DialogDescriptor.OK_OPTION, 0, null)
                );
        
        verify(saveAction).actionPerformed(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertTrue(eventCaptor.getValue().getSource() instanceof PreferencesPanel);
    }
    
    @Test
    public void itShouldNotCallTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        descriptorCaptor.getValue().getButtonListener().actionPerformed(
                new ActionEvent(DialogDescriptor.CANCEL_OPTION, 0, null)
                );
        
        verifyZeroInteractions(saveAction);
    }
    
    private PreferencesPanel getCapturedPrefPanel() {
        assertNotNull(descriptorCaptor.getValue());
        PreferencesPanel panel = (PreferencesPanel)descriptorCaptor.getValue().getMessage();
        assertNotNull(panel);
        return panel;
    }
}
