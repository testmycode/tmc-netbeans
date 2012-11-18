package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.ObsoleteClientException;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
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
    @ActionReference(path = "Menu/TM&C", position = -50)
})
@Messages("CTL_CheckForNewExercisesOrUpdates=&Check for new exercises / updates")
public class CheckForNewExercisesOrUpdates extends AbstractAction {

    public static void startTimer() {
        int interval = 20*60*1000; // 20 minutes
        javax.swing.Timer timer = new javax.swing.Timer(interval, new CheckForNewExercisesOrUpdates(true, true, false));
        timer.setRepeats(true);
        timer.start();
    }
    
    
    private CourseDb courseDb;
    private ServerAccess serverAccess;
    private NotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean beQuiet;
    private boolean backgroundCheck;
    private boolean getCompleted;

    public CheckForNewExercisesOrUpdates() {
        this(false, false, true);
    }

    public CheckForNewExercisesOrUpdates(boolean beQuiet, boolean backgroundCheck, boolean getCompleted) {
        this.courseDb = CourseDb.getInstance();
        this.serverAccess = new ServerAccess();
        this.notifier = NotificationDisplayer.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.beQuiet = beQuiet;
        this.backgroundCheck = backgroundCheck;
        this.getCompleted = getCompleted;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        final Course currentCourse = courseDb.getCurrentCourse();
        
        if (backgroundCheck && !TmcSettings.getDefault().isCheckingForUpdatesInTheBackground()) {
            return;
        }
        
        if (currentCourse == null) {
            if (!beQuiet) {
                dialogs.displayMessage("Please select a course in TMC -> Settings.");
            }
            return;
        }
        
        BgTask.start("Checking for new exercises", serverAccess.getDownloadingCourseListTask(), new BgTaskListener<List<Course>>() {
            @Override
            public void bgTaskReady(List<Course> receivedCourseList) {
                Course receivedCourse = CourseListUtils.getCourseByName(receivedCourseList, currentCourse.getName());
                if (receivedCourse != null) {
                    courseDb.setAvailableCourses(receivedCourseList);

                    final LocalExerciseStatus status = LocalExerciseStatus.get(receivedCourse.getExercises(), getCompleted);
                    if (status.thereIsSomethingToDownload()) {
                        if (beQuiet) {
                            displayNotification(status, new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    DownloadOrUpdateExercisesDialog.display(status.downloadable, status.updateable);
                                }
                            });
                        } else {
                            DownloadOrUpdateExercisesDialog.display(status.downloadable, status.updateable);
                        }
                    } else if (!beQuiet) {
                        dialogs.displayMessage("No new exercises or updates to download.");
                    }
                }
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                if (!beQuiet || ex instanceof ObsoleteClientException) {
                    dialogs.displayError("Failed to check for new exercises.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
                }
            }
        });
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
