package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesPanel;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
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
    
    @Mock private ModalDialogDisplayer dialogDisplayer;
    @Mock private PreferencesPanel panel;
    private SaveSettingsAction action;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        action = new SaveSettingsAction(serverAccess, localCourseCache, projectMediator, dialogDisplayer);
    }
    
    private void performTheAction() {
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null));
    }
    
    @Test
    public void itShouldSaveTheUsername() {
        when(panel.getUsername()).thenReturn("JaneShepard");
        performTheAction();
        verify(serverAccess).setUsername("JaneShepard");
    }
    
    @Test
    public void itShouldSaveTheBaseUrl() {
        when(panel.getServerBaseUrl()).thenReturn("http://www.example.com");
        performTheAction();
        verify(serverAccess).setBaseUrl("http://www.example.com");
    }
    
    @Test
    public void itShouldSaveTheProjectDirectory() {
        when(panel.getProjectDir()).thenReturn("/foo/bar");
        performTheAction();
        verify(projectMediator).setProjectDir("/foo/bar");
    }
    
    @Test
    public void itShouldSaveTheSelectedCourse() {
        Course course = mock(Course.class);
        when(panel.getSelectedCourse()).thenReturn(course);
        performTheAction();
        verify(localCourseCache).setCurrentCourse(course);
    }
    
}
