package fi.helsinki.cs.tmc.actions;

import org.junit.After;
import java.io.IOException;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import org.mockito.Captor;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class OpenExercisesActionTest {
    
    @Mock private ServerAccess serverAccess;
    @Mock private LocalCourseCache courseCache;
    @Mock private ProjectMediator projectMediator;
    @Mock private ConvenientDialogDisplayer dialogs;
    
    private Course currentCourse;
    private ExerciseCollection threeExercises;
    
    @Captor private ArgumentCaptor<BgTaskListener<ExerciseCollection>> exListListenerCaptor;
    @Captor private ArgumentCaptor<BgTaskListener<TmcProjectInfo>> projListenerCaptor;
    
    private OpenExercisesAction action;
    
    private Level oldLogLevel;
    
    @Before
    public void setUp() {
        oldLogLevel = getClassLogger().getLevel();
        getClassLogger().setLevel(Level.OFF);
        
        MockitoAnnotations.initMocks(this);
        currentCourse = new Course("MyCourse");
        when(courseCache.getCurrentCourse()).thenReturn(currentCourse);
        
        threeExercises = new ExerciseCollection();
        threeExercises.add(new Exercise("one"));
        threeExercises.add(new Exercise("two"));
        threeExercises.add(new Exercise("three"));
        
        action = new OpenExercisesAction(
                serverAccess,
                courseCache,
                projectMediator,
                dialogs
                );
    }
    
    @After
    public void tearDown() {
        getClassLogger().setLevel(oldLogLevel);
    }
    
    private Logger getClassLogger() {
        return Logger.getLogger(OpenExercisesAction.class.getName());
    }
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    
    @Test
    public void itShouldRefreshAvailableExercises() {
        performAction();
        respondWithThreeExercises();
        
        verify(courseCache).setAvailableExercises(threeExercises);
    }
    
    @Test
    public void itShouldDownloadExercisesNotYetDownloaded() {
        TmcProjectInfo proj1 = mock(TmcProjectInfo.class);
        TmcProjectInfo proj2 = mock(TmcProjectInfo.class);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(null);
        
        performAction();
        respondWithThreeExercises();
        
        verifyStartedProjDownload(threeExercises.get(2));
        verifyNoMoreInteractions(serverAccess);
        
        verify(proj1).open();
        verify(proj2).open();
    }
    
    @Test
    public void itShouldImmediatelyOpenAllLocalExercises() {
        TmcProjectInfo proj1 = mock(TmcProjectInfo.class);
        TmcProjectInfo proj2 = mock(TmcProjectInfo.class);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(null);
        
        performAction();
        respondWithThreeExercises();
        
        verify(proj1).open();
        verify(proj2).open();
    }
    
    @Test
    public void whenAnExerciseIsDownloadedItShouldOpenIt() {
        performAction();
        respondWithThreeExercises();
        
        TmcProjectInfo proj = mock(TmcProjectInfo.class);
        verifyStartedProjDownload(threeExercises.get(1)).backgroundTaskReady(proj);
        
        verify(proj).open();
    }
    
    @Test
    public void whenAnExerciseDownloadFailsItShouldDisplayAnError() {
        performAction();
        respondWithThreeExercises();
        verifyStartedProjDownload(threeExercises.get(1)).backgroundTaskFailed(new IOException("oops"));
        verify(dialogs).displayError("Failed to download exercise 'two': oops");
    }
    
    @Test
    public void whenAnExerciseDownloadIsCancelledItShouldDoNothing() {
        performAction();
        respondWithThreeExercises();
        verifyStartedProjDownload(threeExercises.get(1)).backgroundTaskCancelled();
    }
    
    @Test
    public void whenNoCourseIsSelectedItShouldOnlyShowAnError() {
        when(courseCache.getCurrentCourse()).thenReturn(null);
        
        performAction();
        
        verify(dialogs).displayError(contains("No course selected"));
        verify(serverAccess, never()).startDownloadingExerciseList(any(Course.class), any(BgTaskListener.class));
        verify(courseCache, never()).setAvailableExercises(any(ExerciseCollection.class));
    }
    
    @Test
    public void whenTheCourseListDownloadFailsItShouldShowAWarningAndOpenAllLocalExercises() {
        when(courseCache.getAvailableExercises()).thenReturn(threeExercises);
        TmcProjectInfo proj1 = mock(TmcProjectInfo.class);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1);
        
        performAction();
        verifyStartedExListDownload().backgroundTaskFailed(new IOException("oops"));
        
        verify(dialogs).displayWarning(any(String.class));
        verify(proj1).open();
        
        verify(courseCache, never()).setAvailableExercises(any(ExerciseCollection.class));
        verify(serverAccess, never()).startDownloadingExerciseProject(any(Exercise.class), any(BgTaskListener.class));
    }
    
    @Test
    public void whenTheCourseListDownloadIsCancelledItShouldDoNothing() {
        performAction();
        verifyStartedExListDownload().backgroundTaskCancelled();
        
        verify(dialogs, never()).displayWarning(any(String.class));
        verify(courseCache, never()).setAvailableExercises(any(ExerciseCollection.class));
        verify(serverAccess, never()).startDownloadingExerciseProject(any(Exercise.class), any(BgTaskListener.class));
    }
    
    private void respondWithThreeExercises() {
        BgTaskListener<ExerciseCollection> listener = verifyStartedExListDownload();
        listener.backgroundTaskReady(threeExercises);
    }
    
    private BgTaskListener<ExerciseCollection> verifyStartedExListDownload() {
        verify(serverAccess).startDownloadingExerciseList(same(currentCourse), exListListenerCaptor.capture());
        return exListListenerCaptor.getValue();
    }
    
    private BgTaskListener<TmcProjectInfo> verifyStartedProjDownload(Exercise exercise) {
        verify(serverAccess).startDownloadingExerciseProject(same(exercise), projListenerCaptor.capture());
        return projListenerCaptor.getValue();
    }
}
