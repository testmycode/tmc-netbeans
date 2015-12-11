package fi.helsinki.cs.tmc.actions;

import static java.util.logging.Level.INFO;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.ObsoleteClientException;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import fi.helsinki.cs.tmc.utilities.Inflector;
import fi.helsinki.cs.tmc.utilities.TmcStringUtils;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.StringUtils;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Icon;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates")
@ActionRegistration(displayName = "#CTL_CheckForNewExercisesOrUpdates")
@ActionReferences({@ActionReference(path = "Menu/TM&C", position = -50)})
@Messages("CTL_CheckForNewExercisesOrUpdates=&Download/update exercises")
public class CheckForNewExercisesOrUpdates extends AbstractAction {

    public static void startTimer() {
        int interval = 20 * 60 * 1000; // 20 minutes
        javax.swing.Timer timer =
                new javax.swing.Timer(interval, new CheckForNewExercisesOrUpdates(true, true));
        timer.setRepeats(true);
        timer.start();
    }

    private static final Logger logger =
            Logger.getLogger(CheckForNewExercisesOrUpdates.class.getName());

    private static final TmcNotificationDisplayer.SingletonToken notifierToken =
            TmcNotificationDisplayer.createSingletonToken();

    private CourseDb courseDb;
    private TmcNotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean beQuiet;
    private boolean backgroundCheck;
    private final TmcCore tmcCore;

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
        final Course currentCourseBeforeUpdate = courseDb.getCurrentCourse();

        BgTaskListener bgTaskListener =
                new BgTaskListener<Course>() {
                    @Override
                    public void bgTaskReady(Course receivedCourse) {
                        if (receivedCourse != null) {

                            courseDb.putDetailedCourse(receivedCourse);

                            final LocalExerciseStatus status =
                                    LocalExerciseStatus.get(receivedCourse.getExercises());

                            if (status.thereIsSomethingToDownload(false)) {
                                if (beQuiet) {
                                    displayNotification(
                                            status,
                                            new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                    DownloadOrUpdateExercisesDialog.display(
                                                            status.unlockable,
                                                            status.downloadableUncompleted,
                                                            status.updateable);
                                                }
                                            });
                                } else {
                                    DownloadOrUpdateExercisesDialog.display(
                                            status.unlockable,
                                            status.downloadableUncompleted,
                                            status.updateable);
                                }
                            } else if (!beQuiet) {
                                dialogs.displayMessage("No new exercises or updates to download.");
                            }
                        }
                    }

                    @Override
                    public void bgTaskCancelled() {}

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        if (!beQuiet || ex instanceof ObsoleteClientException) {
                            dialogs.displayError(
                                    "Failed to check for new exercises.\n"
                                            + ServerErrorHelper.getServerExceptionMsg(ex));
                        }
                    }
                };

        BgTask.start(
                "Checking for new exercises",
                new CancellableCallable<Course>() {
                    ListenableFuture<Course> currentCourseFuture;

                    @Override
                    public Course call() throws Exception {
                        logger.info("Downloading course to refresh cache");
                        currentCourseFuture =
                                tmcCore.getCourse(currentCourseBeforeUpdate.getDetailsUrl());
                        return currentCourseFuture.get();
                    }

                    @Override
                    public boolean cancel() {
                        logger.log(INFO, "Get course (refresh list) cancelled.");
                        return currentCourseFuture.cancel(true);
                    }
                },
                bgTaskListener);
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

        int total =
                status.unlockable.size()
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
