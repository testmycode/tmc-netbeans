package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.ObsoleteClientException;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import fi.helsinki.cs.tmc.utilities.Inflector;
import fi.helsinki.cs.tmc.utilities.TmcStringUtils;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
        id = "fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates")
@ActionRegistration(displayName = "#CTL_CheckForNewExercisesOrUpdates")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -50)
})
@Messages("CTL_CheckForNewExercisesOrUpdates=&Download/update exercises")
public class CheckForNewExercisesOrUpdates extends AbstractAction {

    public static void startTimer() {
        int interval = 20 * 60 * 1000; // 20 minutes
        javax.swing.Timer timer = new javax.swing.Timer(interval, new CheckForNewExercisesOrUpdates(true, true));
        timer.setRepeats(true);
        timer.start();
    }

    private static final TmcNotificationDisplayer.SingletonToken notifierToken = TmcNotificationDisplayer.createSingletonToken();

    private CourseDb courseDb;
    private TmcNotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean beQuiet;
    private boolean backgroundCheck;
    private TmcCore tmcCore;

    public CheckForNewExercisesOrUpdates() {
        this(false, false);
    }

    public CheckForNewExercisesOrUpdates(boolean beQuiet, boolean backgroundCheck) {
        this.courseDb = CourseDb.getInstance();
        this.notifier = TmcNotificationDisplayer.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.beQuiet = beQuiet;
        this.backgroundCheck = backgroundCheck;
        this.tmcCore = TmcCoreSingleton.getInstance();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        try {
            ProgressHandle exerciseRefresh = ProgressHandleFactory.createSystemHandle(
                    "Checking for new exercises");
            exerciseRefresh.start();
            final Course currentCourseBeforeUpdate = courseDb.getCurrentCourse();
            if (backgroundProcessingOrNoCurrentCourse(currentCourseBeforeUpdate)) {
                return;
            }
            ListenableFuture<Course> currentCourseFuture = this.tmcCore.getCourse(
                    NBTmcSettings.getDefault(), currentCourseBeforeUpdate.getDetailsUrl()
            );
            Futures.addCallback(currentCourseFuture, new UpdateCourseForExerciseUpdate(exerciseRefresh));
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * If there is something at background or no current course is chosen,
     * return true.
     */
    private boolean backgroundProcessingOrNoCurrentCourse(final Course currentCourseBeforeUpdate) {
        if (backgroundCheck && !NBTmcSettings.getDefault().isCheckingForUpdatesInTheBackground()) {
            return true;
        }
        if (currentCourseBeforeUpdate == null) {
            if (!beQuiet) {
                dialogs.displayMessage("Please select a course in TMC -> Settings.");
            }
            return true;
        }
        return false;
    }

    class UpdateCourseForExerciseUpdate implements FutureCallback<Course> {
        
        private ProgressHandle lastAction;

        /**
         * This should be attached to listenableFuture. When future is ready,
         * receivedCourse will be saved to courseDb and view will be updated.
         */
        public UpdateCourseForExerciseUpdate(ProgressHandle lastAction) {
            this.lastAction = lastAction;
        }

        @Override
        public void onSuccess(Course receivedCourse) {
            lastAction.finish();
            if (receivedCourse != null) {
                courseDb.putDetailedCourse(receivedCourse);
                final LocalExerciseStatus status = LocalExerciseStatus.get(receivedCourse.getExercises());
                updateGUI(status);
            }
        }

        private void updateGUI(final LocalExerciseStatus status) {
            boolean thereIsSomethingToDownload = status.thereIsSomethingToDownload(false);
            System.out.println("on ladattavaa: " + thereIsSomethingToDownload);
            if (thereIsSomethingToDownload) {
                if (beQuiet) {
                    displayNotification(status, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            DownloadOrUpdateExercisesDialog.display(status.unlockable, status.downloadableUncompleted, status.updateable);
                        }
                    });
                } else {
                    DownloadOrUpdateExercisesDialog.display(status.unlockable, status.downloadableUncompleted, status.updateable);
                }
            } else if (!beQuiet) {
                dialogs.displayMessage("No new exercises or updates to download.");
            }
        }

        @Override
        public void onFailure(Throwable ex) {
            lastAction.finish();
            if (!beQuiet || ex instanceof ObsoleteClientException) {
                dialogs.displayError("Failed to check for new exercises.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        }
    }

    private void displayNotification(LocalExerciseStatus status, ActionListener action) {
        ArrayList<String> items = new ArrayList<String>();
        ArrayList<String> actions = new ArrayList<String>();

        if (!status.unlockable.isEmpty()) {
            items.add(Inflector.pluralize(status.unlockable.size(), "an unlockable exercise"));
            actions.add("unlock");
        }
        if (!status.downloadableUncompleted.isEmpty()) {
            items.add(Inflector.pluralize(status.downloadableUncompleted.size(), "a new exercise"));
            actions.add("download");
        }
        if (!status.updateable.isEmpty()) {
            items.add(Inflector.pluralize(status.updateable.size(), "an update"));
            actions.add("update");
        }

        int total
                = status.unlockable.size()
                + status.downloadableUncompleted.size()
                + status.updateable.size();

        String msg = TmcStringUtils.joinCommaAnd(items);
        msg += " " + Inflector.pluralize(total, "is") + " available.";
        msg = StringUtils.capitalize(msg);

        String prompt = "Click here to " + TmcStringUtils.joinCommaAnd(actions) + ".";

        notifier.notify(notifierToken, msg, getNotificationIcon(), prompt, action);
    }

    private Icon getNotificationIcon() {
        return ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
    }
}
