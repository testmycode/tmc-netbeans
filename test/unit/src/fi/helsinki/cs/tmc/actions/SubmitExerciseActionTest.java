package fi.helsinki.cs.tmc.actions;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class SubmitExerciseActionTest {
    @Mock private ServerAccess serverAccess;
    @Mock private LocalCourseCache courseCache;
    @Mock private ProjectMediator projectMediator;
    @Mock private SubmissionResultDisplayer resultDisplayer;
    @Mock private ConvenientDialogDisplayer dialogDisplayer;
    @Mock private ExerciseIconAnnotator iconAnnotator;
    
    @Mock private Project nbProject;
    @Mock private TmcProjectInfo tmcProject;
    @Mock private Exercise exercise;
    @Mock private SubmissionResult result;
    
    @Captor private ArgumentCaptor<BgTaskListener<SubmissionResult>> listenerCaptor;
    
    private SubmitExerciseAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        when(projectMediator.wrapProject(nbProject)).thenReturn(tmcProject);
        when(result.getStatus()).thenReturn(SubmissionResult.Status.OK);
        
        initAction();
    }
    
    private void initAction() {
        initAction(Arrays.asList(nbProject));
    }
    
    private void initAction(List<Project> projects) {
        action = new SubmitExerciseAction(
                projects,
                serverAccess,
                courseCache,
                projectMediator,
                resultDisplayer,
                dialogDisplayer,
                iconAnnotator);
    }
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    @Test
    public void itShouldSaveAllFilesAndSubmitTheSelectedProjects() {
        when(projectMediator.getMainProject()).thenReturn(tmcProject);
        when(projectMediator.tryGetExerciseForProject(tmcProject, courseCache)).thenReturn(exercise);
        
        performAction();
        
        verify(projectMediator).saveAllFiles();
        verify(serverAccess).startSubmittingExercise(same(exercise), any(BgTaskListener.class));
    }
    
    @Test
    public void whenNoProjectsAreSelectedItShouldDoNothing() {
        initAction(new ArrayList<Project>());
        
        performAction();
        
        verify(projectMediator, never()).saveAllFiles();
        verifyZeroInteractions(serverAccess);
    }
    
    @Test
    public void whenNoExerciseMatchesTheSelectedProjectItShouldDoNothing() {
        when(projectMediator.tryGetExerciseForProject(tmcProject, courseCache)).thenReturn(null);
        
        performAction();
        
        verify(projectMediator, never()).saveAllFiles();
        verifyZeroInteractions(serverAccess);
    }
    
    private void performActionAndCaptureListener() {
        when(projectMediator.tryGetExerciseForProject(tmcProject, courseCache)).thenReturn(exercise);
        
        performAction();
        
        verify(serverAccess).startSubmittingExercise(same(exercise), listenerCaptor.capture());
    }
    
    @Test
    public void whenTheServerReturnsAResultItShouldDisplayIt() {
        performActionAndCaptureListener();
        
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(resultDisplayer).showResult(result);
    }
    
    @Test
    public void whenTheServerReturnsASuccessfulResultItShouldSetTheExerciseStatusToDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.OK);
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsTestFailuresResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.FAIL);
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise, never()).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsAnErrorResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.ERROR);
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise, never()).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheSubmissionIsCancelledItShouldDoNothing() {
        performActionAndCaptureListener();
        listenerCaptor.getValue().backgroundTaskCancelled();
        
        verifyZeroInteractions(resultDisplayer, exercise, iconAnnotator);
    }
    
    @Test
    public void whenTheSubmissionCannotBeCompletedItShouldDisplayAnError() {
        performActionAndCaptureListener();
        Throwable exception = new Exception("oops");
        listenerCaptor.getValue().backgroundTaskFailed(exception);
        
        verify(dialogDisplayer).displayError(exception);
        verifyZeroInteractions(resultDisplayer, exercise, iconAnnotator);
    }
}
