package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseProgress;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ExerciseIconAnnotator;
import fi.helsinki.cs.tmc.ui.SubmissionResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ConvenientDialogDisplayer;
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
    
    @Mock private TmcProjectInfo project;
    @Mock private Exercise exercise;
    @Mock private SubmissionResult result;
    
    @Captor private ArgumentCaptor<BgTaskListener<SubmissionResult>> listenerCaptor;
    
    private SubmitExerciseAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        when(result.getStatus()).thenReturn(SubmissionResult.Status.OK);
        
        action = new SubmitExerciseAction(
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
    public void itShouldSaveAllFilesAndSubmitTheMainProject() {
        when(projectMediator.getMainProject()).thenReturn(project);
        when(projectMediator.tryGetExerciseForProject(project, courseCache)).thenReturn(exercise);
        
        performAction();
        
        verify(projectMediator).saveAllFiles();
        verify(serverAccess).startSubmittingExercise(same(exercise), any(BgTaskListener.class));
    }
    
    @Test
    public void whenNoMainProjectIsSelectedItShouldDoNothing() {
        when(projectMediator.getMainProject()).thenReturn(null);
        
        performAction();
        
        verify(projectMediator, never()).saveAllFiles();
        verifyZeroInteractions(serverAccess);
    }
    
    @Test
    public void whenNoExerciseMatchesTheMainProjectItShouldDoNothing() {
        when(projectMediator.getMainProject()).thenReturn(null);
        when(projectMediator.tryGetExerciseForProject(project, courseCache)).thenReturn(null);
        
        performAction();
        
        verify(projectMediator, never()).saveAllFiles();
        verifyZeroInteractions(serverAccess);
    }
    
    private void performActionAndCaptureListener() {
        when(projectMediator.getMainProject()).thenReturn(project);
        when(projectMediator.tryGetExerciseForProject(project, courseCache)).thenReturn(exercise);
        
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
        
        verify(exercise).setProgress(ExerciseProgress.DONE);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsTestFailuresResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.FAIL);
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(exercise).setProgress(ExerciseProgress.PARTIALLY_DONE);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsAnErrorResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.ERROR);
        listenerCaptor.getValue().backgroundTaskReady(result);
        
        verify(exercise).setProgress(ExerciseProgress.PARTIALLY_DONE);
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
