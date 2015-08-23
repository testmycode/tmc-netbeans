package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadCompletedExercises")
@ActionRegistration(displayName = "#CTL_DownloadCompletedExercises")
@ActionReference(path = "Menu/TM&C", position = -45)
@Messages("CTL_DownloadCompletedExercises=Download old completed exercises")
public final class DownloadCompletedExercises implements ActionListener {

    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;

    public DownloadCompletedExercises() {
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Course currentCourse = courseDb.getCurrentCourse();
        if (currentCourse == null) {
            dialogs.displayMessage("Please select a course in TMC -> Settings.");
            return;
        }

        RefreshCoursesAction action = new RefreshCoursesAction();
        action.addDefaultListener(true, true);
        action.addListener(new BgTaskListener<List<Course>>() {

            @Override
            public void bgTaskReady(List<Course> result) {
                LocalExerciseStatus status = LocalExerciseStatus.get(courseDb.getCurrentCourseExercises());
                if (!status.downloadableCompleted.isEmpty()) {
                    List<Exercise> emptyList = Collections.emptyList();
                    DownloadOrUpdateExercisesDialog.display(emptyList, status.downloadableCompleted, emptyList);
                } else {
                    dialogs.displayMessage("No completed exercises to download.\nDid you only close them and not delete them?");
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
        action.run();
    }
}