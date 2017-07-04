package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.exceptions.ObsoleteClientException;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesWithWeekDialog;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.Inflector;
import fi.helsinki.cs.tmc.utilities.TmcStringUtils;

import org.apache.commons.lang3.StringUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.Icon;

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

    private static final TmcNotificationDisplayer.SingletonToken NOTIFIER_TOKEN = TmcNotificationDisplayer.createSingletonToken();

    private CourseDb courseDb;
    private TmcNotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean beQuiet;
    private boolean backgroundCheck;
    private TmcEventBus eventBus;

    public CheckForNewExercisesOrUpdates() {
        this(false, false);
    }

    public CheckForNewExercisesOrUpdates(boolean beQuiet, boolean backgroundCheck) {
        this.courseDb = CourseDb.getInstance();
        this.notifier = TmcNotificationDisplayer.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.beQuiet = beQuiet;
        this.backgroundCheck = backgroundCheck;
        this.eventBus = TmcEventBus.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        final Course currentCourseBeforeUpdate = courseDb.getCurrentCourse();

        if (backgroundCheck && !((TmcCoreSettingsImpl)TmcSettingsHolder.get()).isCheckingForUpdatesInTheBackground()) {
            return;
        }

        if (currentCourseBeforeUpdate == null) {
            if (!beQuiet) {
                dialogs.displayMessage("Please select a course in TMC -> Settings.");
            }
            return;
        }
        eventBus.post(new InvokedEvent(currentCourseBeforeUpdate));

        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Course> getFullCourseInfoTask = TmcCore.get().getCourseDetails(observer, courseDb.getCurrentCourse());
        BgTask.start("Checking for new exercises", getFullCourseInfoTask, observer, new BgTaskListener<Course>() {
            @Override
            public void bgTaskReady(final Course receivedCourse) {
                if (receivedCourse != null) {
                    courseDb.putDetailedCourse(receivedCourse);

                    final LocalExerciseStatus status = LocalExerciseStatus.get(receivedCourse.getExercises());

                    if (status.thereIsSomethingToDownload(false)) {
                        if (beQuiet) {
                            displayNotification(status, new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    DownloadOrUpdateExercisesWithWeekDialog.display(status.unlockable, status.downloadableUncompleted, status.updateable);
                                }
                            });
                        } else {
                            DownloadOrUpdateExercisesWithWeekDialog.display(status.unlockable, status.downloadableUncompleted, status.updateable);
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

        notifier.notify(NOTIFIER_TOKEN, msg, getNotificationIcon(), prompt, action);
    }

    private Icon getNotificationIcon() {
        return ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
    }

    public static class InvokedEvent implements TmcEvent {

        public Course course;

        public InvokedEvent(Course currentCourse) {
            this.course = currentCourse;
        }
    }
}
