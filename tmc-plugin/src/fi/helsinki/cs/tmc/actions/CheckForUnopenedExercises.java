 package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForUnopenedExercises implements ActionListener {
    public static boolean shouldRunOnStartup() {
        return NbTmcSettings.getDefault().isCheckingForUnopenedAtStartup();
    }
    
    private static final TmcNotificationDisplayer.SingletonToken notifierToken = TmcNotificationDisplayer.createSingletonToken();
    
    private ProjectMediator projects;
    private CourseDb courseDb;
    private TmcNotificationDisplayer notifier;

    public CheckForUnopenedExercises() {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.notifier = TmcNotificationDisplayer.getDefault();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        // TODO(jamo): use bg task
        projects.callWhenProjectsCompletelyOpened(new Runnable() {
            @Override
            public void run() {
                List<Exercise> unopenedExercises = new ArrayList<Exercise>();
                for (Exercise ex : courseDb.getCurrentCourseExercises()) {
                    TmcProjectInfo project = projects.tryGetProjectForExercise(ex);
                    if (project != null && !projects.isProjectOpen(project)) {
                        unopenedExercises.add(ex);
                    }
                }

                if (!unopenedExercises.isEmpty()) {
                    showNotification(unopenedExercises);
                }
            }
        });
    }

    private void showNotification(List<Exercise> unopenedExercises) {
        int count = unopenedExercises.size();
        String msg;
        String prompt;
        if (count == 1) {
            msg = "There is one exercise that is downloaded but not opened.";
            prompt = "Click here to open it.";
        } else {
            msg = "There are " + count + " exercises that are downloaded but not opened.";
            prompt = "Click here to open them.";
        }
        notifier.notify(notifierToken, msg, getNotificationIcon(), prompt, openAction(unopenedExercises), NotificationDisplayer.Priority.LOW);
    }
    
    private ActionListener openAction(final List<Exercise> exercises) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Exercise ex : exercises) {
                    TmcProjectInfo project = projects.tryGetProjectForExercise(ex);
                    if (project != null && !projects.isProjectOpen(project)) {
                        projects.openProject(project);
                    }
                }
            }
        };
    }
    
    private Icon getNotificationIcon() {
        return ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false);
    }
}
