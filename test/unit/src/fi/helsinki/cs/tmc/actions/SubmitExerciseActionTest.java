package fi.helsinki.cs.tmc.actions;

import java.util.logging.Logger;
import org.junit.AfterClass;
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
import java.util.logging.Level;
import org.junit.Before;
import org.junit.BeforeClass;
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
    
    private static Level oldSCOLogLevel;
    
    @BeforeClass
    public static void setUpClass() {
        Logger scoLog = Logger.getLogger("org.openide.util.SharedClassObject");
        oldSCOLogLevel = scoLog.getLevel();
        scoLog.setLevel(Level.OFF); // To avoid warning about multiple instances of the action being created
    }
    
    @AfterClass
    public static void tearDownClass() {
        Logger.getLogger("org.openide.util.SharedClassObject").setLevel(oldSCOLogLevel);
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        when(projectMediator.wrapProject(nbProject)).thenReturn(tmcProject);
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
        performActionWithProjects(nbProject);
    }
    
    private void performActionWithProjects(Project ... projects) {
        action.performAction(projects);
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
        performActionWithProjects();
        
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
        
        listenerCaptor.getValue().bgTaskReady(result);
        
        verify(resultDisplayer).showResult(result);
    }
    
    @Test
    public void whenTheServerReturnsASuccessfulResultItShouldSetTheExerciseStatusToDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.OK);
        listenerCaptor.getValue().bgTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsTestFailuresResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.FAIL);
        listenerCaptor.getValue().bgTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise, never()).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheServerReturnsAnErrorResultItShouldSetTheExerciseStatusToPartiallyDone() {
        performActionAndCaptureListener();
        when(result.getStatus()).thenReturn(SubmissionResult.Status.ERROR);
        listenerCaptor.getValue().bgTaskReady(result);
        
        verify(exercise).setAttempted(true);
        verify(exercise, never()).setCompleted(true);
        verify(iconAnnotator).updateAllIcons();
        verify(courseCache).save();
    }
    
    @Test
    public void whenTheSubmissionIsCancelledItShouldDoNothing() {
        performActionAndCaptureListener();
        listenerCaptor.getValue().bgTaskCancelled();
        
        verifyZeroInteractions(resultDisplayer, exercise, iconAnnotator);
    }
    
    @Test
    public void whenTheSubmissionCannotBeCompletedItShouldDisplayAnError() {
        performActionAndCaptureListener();
        Throwable exception = new Exception("oops");
        listenerCaptor.getValue().bgTaskFailed(exception);
        
        verify(dialogDisplayer).displayError(exception);
        verifyZeroInteractions(resultDisplayer, exercise, iconAnnotator);
    }
}
