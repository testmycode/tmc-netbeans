package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import java.awt.event.ActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class SaveSettingsActionTest {
    @Mock private TmcServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private ProjectMediator projectMediator;
    
    @Mock private PreferencesUI prefUi;
    private SaveSettingsAction action;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        action = new SaveSettingsAction(serverAccess, localCourseCache, projectMediator);
    }
    
    private void performTheAction() {
        action.actionPerformed(new ActionEvent(prefUi, ActionEvent.ACTION_PERFORMED, null));
    }
    
    @Test
    public void itShouldSaveTheUsername() {
        when(prefUi.getUsername()).thenReturn("JaneShepard");
        performTheAction();
        verify(serverAccess).setUsername("JaneShepard");
    }
    
    @Test
    public void itShouldSaveTheBaseUrl() {
        when(prefUi.getServerBaseUrl()).thenReturn("http://www.example.com");
        performTheAction();
        verify(serverAccess).setBaseUrl("http://www.example.com");
    }
    
    @Test
    public void itShouldSaveTheProjectDirectory() {
        when(prefUi.getProjectDir()).thenReturn("/foo/bar");
        performTheAction();
        verify(projectMediator).setProjectDir("/foo/bar");
    }
    
    @Test
    public void itShouldSaveTheSelectedCourse() {
        Course course = new Course("xoo");
        when(prefUi.getSelectedCourse()).thenReturn(course);
        performTheAction();
        verify(localCourseCache).setCurrentCourseName("xoo");
    }
    
    @Test
    public void itShouldSaveTheFactThatNoCourseIsSelected() {
        when(prefUi.getSelectedCourse()).thenReturn(null);
        performTheAction();
        verify(localCourseCache).setCurrentCourseName(null);
    }
    
}
