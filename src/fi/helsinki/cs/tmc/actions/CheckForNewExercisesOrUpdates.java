package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates")
@ActionRegistration(displayName = "#CTL_CheckForNewExercisesOrUpdates")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -50, separatorAfter = -40)
})
@Messages("CTL_CheckForNewExercisesOrUpdates=&Check for new exercises / updates")
public class CheckForNewExercisesOrUpdates extends AbstractAction {
    private CourseDb courseDb;
    private ServerAccess serverAccess;
    private NotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean tellIfNothingToDownload;

    public CheckForNewExercisesOrUpdates() {
        this(true);
    }
    
    public CheckForNewExercisesOrUpdates(boolean tellIfNothingToDownload) {
        this.courseDb = CourseDb.getInstance();
        this.serverAccess = new ServerAccess();
        this.notifier = NotificationDisplayer.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.tellIfNothingToDownload = tellIfNothingToDownload;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        final Course currentCourse = courseDb.getCurrentCourse();
        if (currentCourse != null) {
            BgTask.start("Checking for new exercises", serverAccess.getDownloadingCourseListTask(), new BgTaskListener<List<Course>>() {
                @Override
                public void bgTaskReady(List<Course> receivedCourseList) {
                    Course receivedCourse = CourseListUtils.getCourseByName(receivedCourseList, currentCourse.getName());
                    if (receivedCourse != null) {
                        courseDb.setAvailableCourses(receivedCourseList);

                        final LocalExerciseStatus status = LocalExerciseStatus.get(receivedCourse.getExercises());
                        if (status.thereIsSomethingToDownload()) {
                            displayNotification(status, new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    DownloadOrUpdateExercisesDialog.display(status.downloadable, status.updateable);
                                }
                            });
                        } else if (tellIfNothingToDownload) {
                            dialogs.displayMessage("No new exercises or updates to download.");
                        }
                    }
                }

                @Override
                public void bgTaskCancelled() {
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    dialogs.displayError("Failed to check for new exercises.\n" + DownloadErrorHelper.getDownloadExceptionMsg(ex));
                }
            });
        }
    }

    private void displayNotification(LocalExerciseStatus status, ActionListener action) {
        String ds = status.downloadable.size() > 1 ? "s" : "";
        String us = status.updateable.size() > 1 ? "s" : "";
        
        String msg;
        String prompt;
        if (status.downloadable.size() > 0 && status.updateable.size() > 0) {
            msg = status.downloadable.size() + " new exercise" + ds + " and " + status.updateable.size() + " update" + us + " are available.";
            prompt = "Click here to download and update";
        } else if (status.downloadable.size() > 0) {
            msg = status.downloadable.size() + " new exercise" + ds + " are available.";
            prompt = "Click here to download";
        } else {
            msg = status.updateable.size() + " exercise" + us + " can be updated.";
            prompt = "Click here to update";
        }
        notifier.notify(msg, getNotificationIcon(), prompt, action);
    }

    private Icon getNotificationIcon() {
        return ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
    }
}
