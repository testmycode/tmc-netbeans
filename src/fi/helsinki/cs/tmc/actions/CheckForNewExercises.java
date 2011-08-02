package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForNewExercises implements ActionListener {
    private LocalCourseCache courseCache;
    private ProjectMediator projectMediator;
    private ServerAccess serverAccess;
    private NotificationDisplayer notifier;
    private ActionListener detailsAction;

    public CheckForNewExercises(ActionListener detailsAction) {
        this(LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                ServerAccess.getDefault(),
                NotificationDisplayer.getDefault(),
                detailsAction);
    }
    
    public CheckForNewExercises(
            LocalCourseCache courseCache,
            ProjectMediator projectMediator,
            ServerAccess serverAccess,
            NotificationDisplayer notifier,
            ActionListener detailsAction) {
        this.courseCache = courseCache;
        this.projectMediator = projectMediator;
        this.serverAccess = serverAccess;
        this.notifier = notifier;
        this.detailsAction = detailsAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Course currentCourse = courseCache.getCurrentCourse();
        if (currentCourse != null) {
            serverAccess.startDownloadingCourseList(new BgTaskListener<CourseList>() {
                @Override
                public void bgTaskReady(CourseList receivedCourseList) {
                    Course receivedCourse = receivedCourseList.getCourseByName(currentCourse.getName());
                    if (receivedCourse != null) {
                        int count = countUndownloadedExercisesInCourse(receivedCourse);
                        if (count > 0) {
                            courseCache.setAvailableCourses(receivedCourseList);
                            displayNotification(count);
                        }
                    }
                }

                @Override
                public void bgTaskCancelled() {
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                }
            });
        }
    }
    
    private int countUndownloadedExercisesInCourse(Course receivedCourse) {
        int count = 0;
        for (Exercise ex : receivedCourse.getExercises()) {
            if (projectMediator.tryGetProjectForExercise(ex) == null) {
                count += 1;
            }
        }
        return count;
    }
    
    private void displayNotification(int count) {
        Icon icon = ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
        notifier.notify("New exercises are available (" + count + ")", icon, "Click here to download and open them.", detailsAction);
    }
}
