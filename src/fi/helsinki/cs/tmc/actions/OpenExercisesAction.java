package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseList;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
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
    private CourseDb courseDb;
    private NbProjectUnzipper unzipper;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;

    public OpenExercisesAction() {
        this.serverAccess = ServerAccess.create();
        this.courseDb = CourseDb.getInstance();
        this.unzipper = NbProjectUnzipper.getDefault();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Course course = courseDb.getCurrentCourse();
        
        if (course == null) {
            dialogs.displayError("No course selected. Please select one in TMC -> Settings");
            return;
        }
        
        int localProjects = openLocalProjects(courseDb.getCurrentCourseExercises());
        refreshProjectListAndDownloadNewProjects(localProjects);
    }
    
    private int openLocalProjects(ExerciseList exercises) {
        ArrayList<TmcProjectInfo> projects = new ArrayList<TmcProjectInfo>();
        for (Exercise ex : exercises) {
            TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
            if (proj != null && !projectMediator.isProjectOpen(proj)) {
                projects.add(proj);
            }
        }
        
        projectMediator.openProjects(projects);
        
        return projects.size();
    }
    
    private void refreshProjectListAndDownloadNewProjects(final int localProjectCount) {
        serverAccess.startDownloadingCourseList(new BgTaskListener<CourseList>() {
            @Override
            public void bgTaskReady(CourseList result) {
                courseDb.setAvailableCourses(result);
                downloadNewProjects(courseDb.getCurrentCourseExercises(), localProjectCount);
            }

            @Override
            public void bgTaskCancelled() {
                // Do nothing
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                downloadNewProjects(courseDb.getCurrentCourseExercises(), localProjectCount);
            }
        });
    }
    
    private void downloadNewProjects(ExerciseList allExercises, final int localProjectCount) {
        ExerciseList exercisesToDownload = undownloadedUnexpiredExercises(allExercises);
        
        if (exercisesToDownload.size() == 0 && localProjectCount == 0) {
            dialogs.displayMessage("There are no new exercises at the moment.");
        }
        
        BgTaskListener<Collection<TmcProjectInfo>> whenFinished = new BgTaskListener<Collection<TmcProjectInfo>>() {
            @Override
            public void bgTaskReady(Collection<TmcProjectInfo> result) {
                projectMediator.openProjects(result);
            }

            @Override
            public void bgTaskCancelled() {
                // Do nothing
            }

            @Override
            public void bgTaskFailed(Throwable exception) {
                logger.log(Level.INFO, "Failed to download exercise file.", exception);
                dialogs.displayError("Failed to download exercises: " + exception.getMessage());
            }
        };
        
        final AggregatingBgTaskListener<TmcProjectInfo> aggregator =
                new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToDownload.size(), whenFinished);
        
        for (final Exercise exercise : exercisesToDownload) {
            startDownloadingAndUnzippingExercise(exercise, aggregator);
        }
    }

    private void startDownloadingAndUnzippingExercise(final Exercise exercise, final BgTaskListener<TmcProjectInfo> listener) {
        serverAccess.startDownloadingExerciseZip(exercise, new BgTaskListener<byte[]>() {
            @Override
            public void bgTaskReady(final byte[] zipData) {
                BgTask.start("Extracting project", listener, new Callable<TmcProjectInfo>() {
                    @Override
                    public TmcProjectInfo call() throws Exception {
                        unzipper.unzipProject(zipData, projectMediator.getProjectDirForExercise(exercise), exercise.getName());
                        TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(exercise);
                        if (proj == null) {
                            throw new RuntimeException("Failed to open project for exercise " + exercise.getName());
                        }
                        return proj;
                    }
                });
            }

            @Override
            public void bgTaskCancelled() {
                listener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                listener.bgTaskFailed(ex);
            }
        });
    }
    
    private ExerciseList undownloadedUnexpiredExercises(ExerciseList exercises) {
        Date now = new Date();
        
        ExerciseList result = new ExerciseList();
        for (final Exercise exercise : exercises) {
            if (!exercise.hasDeadlinePassed(now)) {
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(exercise);
                if (proj == null) {
                    result.add(exercise);
                }
            }
        }
        return result;
    }
    
    //TODO: disabled if no course selected
}
