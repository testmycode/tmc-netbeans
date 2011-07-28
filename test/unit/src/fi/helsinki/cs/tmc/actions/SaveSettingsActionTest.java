package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import java.awt.event.ActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class SaveSettingsActionTest {
    @Mock private ServerAccess serverAccess;
    @Mock private LocalCourseCache localCourseCache;
    @Mock private ProjectMediator projectMediator;
    @Mock private ConvenientDialogDisplayer dialogs;
    @Mock private OpenExercisesAction openExercisesAction;
    @Mock private ExerciseIconAnnotator iconAnnotator;
    
    @Mock private PreferencesUI prefUi;
    private SaveSettingsAction action;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        action = new SaveSettingsAction(
                serverAccess,
                localCourseCache,
                projectMediator,
                dialogs,
                openExercisesAction,
                iconAnnotator);
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
        verify(projectMediator).setProjectRootDir("/foo/bar");
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
    
    @Test
    public void itShouldUpdateProjectIcons() {
        performTheAction();
        verify(iconAnnotator).updateAllIcons();
    }
    
    @Test
    public void whenACourseWasSelectedItShouldAskIfTheUserWantsToOpenExercises() {
        Course course = new Course("TheCourse");
        when(prefUi.getSelectedCourse()).thenReturn(course);
        when(dialogs.askYesNo(any(String.class), any(String.class))).thenReturn(true);
        
        performTheAction();
        
        verify(openExercisesAction, timeout(5000)).actionPerformed(any(ActionEvent.class));
    }
    
    @Test
    public void whenACourseWasNotSelectedItShouldNotAskIfTheUserWantsToOpenExercises() {
        when(prefUi.getSelectedCourse()).thenReturn(null);
        
        performTheAction();
        
        verifyZeroInteractions(dialogs, openExercisesAction);
    }
}
