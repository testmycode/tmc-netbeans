package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.CourseList;
import org.junit.After;
import java.io.IOException;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import org.mockito.Captor;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseList;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.util.Arrays;
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
    @Mock private CourseDb courseDb;
    @Mock private ProjectMediator projectMediator;
    @Mock private ConvenientDialogDisplayer dialogs;
    
    private Course currentCourse;
    private ExerciseList threeExercises;
    
    @Mock private TmcProjectInfo proj1AsLocal, proj2AsLocal, proj3AsLocal;
    @Mock private TmcProjectInfo proj1AsRemote, proj2AsRemote, proj3AsRemote;
    
    @Captor private ArgumentCaptor<BgTaskListener<CourseList>> listListenerCaptor;
    @Captor private ArgumentCaptor<BgTaskListener<TmcProjectInfo>> projListenerCaptor;
    
    private OpenExercisesAction action;
    
    private Level oldLogLevel;
    
    @Before
    public void setUp() {
        oldLogLevel = getClassLogger().getLevel();
        getClassLogger().setLevel(Level.OFF);
        
        MockitoAnnotations.initMocks(this);
        currentCourse = new Course("MyCourse");
        when(courseDb.getCurrentCourse()).thenReturn(currentCourse);
        
        threeExercises = new ExerciseList();
        threeExercises.add(new Exercise("one"));
        threeExercises.add(new Exercise("two"));
        threeExercises.add(new Exercise("three"));
        when(courseDb.getCurrentCourseExercises()).thenReturn(threeExercises);
        
        when(projectMediator.isProjectOpen(any(TmcProjectInfo.class))).thenReturn(false);
        
        action = new OpenExercisesAction(
                serverAccess,
                courseDb,
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
    public void itShouldRefreshTheExerciseListForTheCurrentCourseDb() {
        CourseList courses = new CourseList();
        courses.add(new Course("irrelevant"));
        courses.add(currentCourse);
        
        performAction();
        verify(serverAccess).startDownloadingCourseList(listListenerCaptor.capture());
        listListenerCaptor.getValue().bgTaskReady(courses);
        
        verify(courseDb).setAvailableCourses(courses);
    }
    
    @Test
    public void itShouldOnlyDownloadExercisesNotYetPresent() {
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(null);
        
        performAction();
        respondWithMockCourseList();
        
        verifyStartedProjDownload(threeExercises.get(2));
        verifyNoMoreInteractions(serverAccess);
    }
    
    @Test
    public void whenTheExerciseListCannotBeDownloadedItShouldIgnoreTheErrorAndUseTheOldList() {
        performAction();
        verify(serverAccess).startDownloadingCourseList(listListenerCaptor.capture());
        listListenerCaptor.getValue().bgTaskFailed(new Exception("oops"));
        
        verify(courseDb, never()).setAvailableCourses(any(CourseList.class));
        verifyZeroInteractions(dialogs);
        
        verifyStartedProjDownload(threeExercises.get(2));
    }
    
    @Test
    public void itShouldImmediatelyOpenAllLocalExercises() {
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(null);
        
        performAction();
        
        verify(projectMediator).openProjects(Arrays.asList(proj1AsLocal, proj2AsLocal));
    }
    
    @Test
    public void whenAnExerciseIsDownloadedItShouldOpenIt() {
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(proj3AsLocal);
        
        performAction();
        respondWithMockCourseList();
        
        verifyStartedProjDownload(threeExercises.get(1)).bgTaskReady(proj2AsRemote);
        
        verify(projectMediator).openProjects(Arrays.asList(proj2AsRemote));
    }
    
    @Test
    public void whenAnExerciseDownloadFailsItShouldDisplayAnError() {
        performAction();
        respondWithMockCourseList();
        verifyStartedProjDownload(threeExercises.get(1)).bgTaskFailed(new IOException("oops"));
        verify(dialogs).displayError("Failed to download exercises: oops");
    }
    
    @Test
    public void whenAnExerciseDownloadIsCancelledItShouldDoNothing() {
        performAction();
        respondWithMockCourseList();
        verifyStartedProjDownload(threeExercises.get(1)).bgTaskCancelled();
    }
    
    @Test
    public void whenNoCourseIsSelectedItShouldOnlyShowAnError() {
        when(courseDb.getCurrentCourse()).thenReturn(null);
        
        performAction();
        
        verify(dialogs).displayError(contains("No course selected"));
        verifyZeroInteractions(projectMediator);
    }
    
    @Test
    public void whenThereAreNoLocalNorRemoteExercisesToOpenItShouldDisplayANotification() {
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(proj3AsLocal);
        
        when(projectMediator.isProjectOpen(any(TmcProjectInfo.class))).thenReturn(true);
        
        performAction();
        respondWithMockCourseList();
        
        verify(dialogs).displayMessage(contains("no new exercises"));
    }
    
    @Test
    public void whenThereAreOnlyLocalExercisesToOpenItShouldNotDisplayANotification() {
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(0))).thenReturn(proj1AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(1))).thenReturn(proj2AsLocal);
        when(projectMediator.tryGetProjectForExercise(threeExercises.get(2))).thenReturn(proj3AsLocal);
        
        when(projectMediator.isProjectOpen(any(TmcProjectInfo.class))).thenReturn(false);
        
        performAction();
        respondWithMockCourseList();
        
        verifyZeroInteractions(dialogs);
    }
    
    @Test
    public void whenThereAreOnlyRemoteExercisesToOpenItShouldNotDisplayANotification() {
        when(projectMediator.isProjectOpen(any(TmcProjectInfo.class))).thenReturn(true);
        
        performAction();
        
        when(courseDb.getCurrentCourseExercises()).thenReturn(threeExercises);
        respondWithMockCourseList();
        
        verifyStartedProjDownload(threeExercises.get(0)).bgTaskReady(proj1AsRemote);
        verifyStartedProjDownload(threeExercises.get(1)).bgTaskReady(proj2AsRemote);
        verifyStartedProjDownload(threeExercises.get(2)).bgTaskReady(proj3AsRemote);
        verifyZeroInteractions(dialogs);
    }
    
    private BgTaskListener<TmcProjectInfo> verifyStartedProjDownload(Exercise exercise) {
        verify(serverAccess).startDownloadingExerciseProject(same(exercise), projListenerCaptor.capture());
        return projListenerCaptor.getValue();
    }
    
    private BgTaskListener<CourseList> verifyStartedCourseListDownload() {
        verify(serverAccess).startDownloadingCourseList(listListenerCaptor.capture());
        return listListenerCaptor.getValue();
    }
    
    private void respondWithMockCourseList() {
        verifyStartedCourseListDownload().bgTaskReady(mock(CourseList.class));
    }
}
