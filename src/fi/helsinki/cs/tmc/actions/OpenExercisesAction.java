package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseList;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.OpenExercises")
@ActionRegistration(displayName = "#CTL_OpenExercises")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -50, separatorAfter = -40)
})
@Messages("CTL_OpenExercises=&Open current exercises")
public class OpenExercisesAction extends AbstractAction {
    
    private static final Logger logger = Logger.getLogger(OpenExercisesAction.class.getName());
    
    private ServerAccess serverAccess;
    private LocalCourseCache courseCache;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    public OpenExercisesAction() {
        this(ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                ConvenientDialogDisplayer.getDefault());
    }

    public OpenExercisesAction(
            ServerAccess serverAccess,
            LocalCourseCache courseCache,
            ProjectMediator projectMediator,
            ConvenientDialogDisplayer dialogs) {
        this.serverAccess = serverAccess;
        this.courseCache = courseCache;
        this.projectMediator = projectMediator;
        this.dialogs = dialogs;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Course course = courseCache.getCurrentCourse();
        
        if (course == null) {
            dialogs.displayError("No course selected. Please select one in TMC -> Settings");
            return;
        }
        
        openLocalProjects(courseCache.getCurrentCourseExercises());
        refreshProjectListAndDownloadNewProjects();
    }
    
    private void openLocalProjects(ExerciseList exercises) {
        ArrayList<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>();
        for (Exercise ex : exercises) {
            TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
            if (proj != null) {
                projects.add(proj);
            }
        }
        projectMediator.openProjects(projects);
    }
    
    private void refreshProjectListAndDownloadNewProjects() {
        serverAccess.startDownloadingCourseList(new BgTaskListener<CourseList>() {
            @Override
            public void bgTaskReady(CourseList result) {
                courseCache.setAvailableCourses(result);
                downloadNewProjects(courseCache.getCurrentCourseExercises());
            }

            @Override
            public void bgTaskCancelled() {
                // Do nothing
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                downloadNewProjects(courseCache.getCurrentCourseExercises());
            }
        });
    }
    
    private void downloadNewProjects(ExerciseList exercises) {
        for (final Exercise exercise : exercises) {
            TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(exercise);
            if (proj == null) {
                serverAccess.startDownloadingExerciseProject(exercise, new BgTaskListener<TmcProjectInfo>() {
                    @Override
                    public void bgTaskReady(TmcProjectInfo result) {
                        projectMediator.openProject(result);
                    }

                    @Override
                    public void bgTaskCancelled() {
                        // Do nothing
                    }

                    @Override
                    public void bgTaskFailed(Throwable exception) {
                        logger.log(Level.WARNING, "Failed to download exercise file.", exception);
                        dialogs.displayError("Failed to download exercise '" + exercise.getName() + "': " + exception.getMessage());
                    }
                });
            }
        }
    }
    
    //TODO: disabled if no course selected
}
