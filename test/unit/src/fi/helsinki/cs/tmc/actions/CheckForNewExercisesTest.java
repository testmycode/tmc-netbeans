package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.openide.awt.NotificationDisplayer;

public class CheckForNewExercisesTest {
    @Mock private CourseDb courseDb;
    @Mock private ProjectMediator projectMediator;
    @Mock private ServerAccess serverAccess;
    @Mock private NotificationDisplayer notifier;
    @Mock private ActionListener detailsAction;
    
    private Course currentCourse;
    private Course otherCourse;
    private CourseList bothCourses;
    
    @Captor private ArgumentCaptor<BgTaskListener<CourseList>> listCaptor;
    
    private CheckForNewExercises action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        currentCourse = new Course("CurrentCourse");
        otherCourse = new Course("OtherCourse");
        bothCourses = new CourseList();
        bothCourses.add(currentCourse);
        bothCourses.add(otherCourse);
        
        when(courseDb.getCurrentCourse()).thenReturn(currentCourse);
        
        action = new CheckForNewExercises(
                courseDb,
                projectMediator,
                serverAccess,
                notifier,
                detailsAction
                );
    }
    
    private void performAction() {
        action.actionPerformed(null);
    }
    
    private BgTaskListener<CourseList> downloadTask() {
        verify(serverAccess).startDownloadingCourseList(listCaptor.capture());
        return listCaptor.getValue();
    }
    
    private void mockProjectFor(Exercise exercise) {
        TmcProjectInfo proj = mock(TmcProjectInfo.class);
        when(projectMediator.tryGetProjectForExercise(exercise)).thenReturn(proj);
    }
    
    private void verifyNotificationDisplayed(boolean displayed) {
        verify(notifier, displayed ? atLeastOnce() : never())
                .notify(any(String.class), any(Icon.class), any(String.class), eq(detailsAction));
    }
    
    @Test
    public void whenNewExercisesAreAvailableItShouldDisplayANotificationAndUpdateTheCourseDb() {
        Exercise ex1 = new Exercise();
        Exercise ex2 = new Exercise();
        Exercise ex3 = new Exercise();
        mockProjectFor(ex1);
        currentCourse.getExercises().add(ex1);
        currentCourse.getExercises().add(ex2);
        otherCourse.getExercises().add(ex3);
        
        performAction();
        downloadTask().bgTaskReady(bothCourses);
        
        verifyNotificationDisplayed(true);
        verify(courseDb).setAvailableCourses(bothCourses);
    }
    
    @Test
    public void whenAllExercisesAreAlreadyDownloadedItShouldNotDisplayANotification() {
        Exercise ex1 = new Exercise();
        Exercise ex2 = new Exercise();
        Exercise ex3 = new Exercise();
        mockProjectFor(ex1);
        mockProjectFor(ex2);
        currentCourse.getExercises().add(ex1);
        currentCourse.getExercises().add(ex2);
        otherCourse.getExercises().add(ex3);
        
        performAction();
        downloadTask().bgTaskReady(bothCourses);
        
        verifyNotificationDisplayed(false);
    }
    
    @Test
    public void whenThereIsNoCurrentCourseItShouldDoNothing() {
        when(courseDb.getCurrentCourse()).thenReturn(null);
        performAction();
        verifyZeroInteractions(serverAccess);
        verifyNotificationDisplayed(false);
    }
    
    @Test
    public void whenTheCurrentCourseHasGoneAwayFromTheServerItShouldDoNothing() {
        CourseList serverCourses = new CourseList();
        serverCourses.add(otherCourse);
        
        performAction();
        downloadTask().bgTaskReady(serverCourses);
        
        verifyNotificationDisplayed(false);
    }
    
    @Test
    public void whenTheExerciseListDownloadFailsItShouldDoNothing() {
        performAction();
        downloadTask().bgTaskFailed(new Exception("oops"));
        
        verifyNotificationDisplayed(false);
    }
}
