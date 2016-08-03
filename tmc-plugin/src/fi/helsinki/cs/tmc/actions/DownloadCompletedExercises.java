package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.exceptions.ObsoleteClientException;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadCompletedExercises")
@ActionRegistration(displayName = "#CTL_DownloadCompletedExercises")
@ActionReference(path = "Menu/TM&C", position = -45)
@Messages("CTL_DownloadCompletedExercises=Download old completed exercises")
public final class DownloadCompletedExercises implements ActionListener {

    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;

    public DownloadCompletedExercises() {
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Course currentCourse = courseDb.getCurrentCourse();
        if (currentCourse == null) {
            dialogs.displayMessage("Please select a course in TMC -> Settings.");
            return;
        }

        eventBus.post(new InvokedEvent(currentCourse));

        Callable<Course> getFullCourseInfoTask = TmcCore.get().getCourseDetails(ProgressObserver.NULL_OBSERVER, courseDb.getCurrentCourse());
        BgTask.start("Checking for new exercises", getFullCourseInfoTask, new BgTaskListener<Course>() {
            @Override
            public void bgTaskReady(Course receivedCourse) {
                if (receivedCourse != null) {
                    courseDb.putDetailedCourse(receivedCourse);

                    final LocalExerciseStatus status = LocalExerciseStatus.get(receivedCourse.getExercises());

                    if (!status.downloadableCompleted.isEmpty()) {
                        List<Exercise> emptyList = Collections.emptyList();
                        DownloadOrUpdateExercisesDialog.display(emptyList, status.downloadableCompleted, emptyList);
                    } else {
                        dialogs.displayMessage("No completed exercises to download.\nDid you only close them and not delete them?");
                    }
                }
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError("Failed to check for new exercises.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        });
    }

    public static class InvokedEvent implements TmcEvent {

        public Course course;

        public InvokedEvent(Course currentCourse) {
            this.course = currentCourse;
        }
    }
}
