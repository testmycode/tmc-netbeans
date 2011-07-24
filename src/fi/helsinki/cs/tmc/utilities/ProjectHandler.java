/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities;

import fi.helsinki.cs.tmc.model.LocalCourseCache;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.settings.LegacyPluginSettings;

/**
 *
 * @author ttkoivis
 */
public class ProjectHandler {

    /**
     * Constructor
     */
    public ProjectHandler() {
    }

    
    
    /**
     * Get main project path.
     * @return String Main project path.
     */
    public static String getMainProjectPath() {
        Project project = OpenProjects.getDefault().getMainProject();

        if (project == null) {
            return null;
        }

        return project.getProjectDirectory().getPath();
    }

    
    
    /**
     * Checks if MainProject is an exercise from host address.
     * @param String Project path.
     * @return boolean True if MainProject is an exercise.
     */
    public static boolean isExercise(String projectPath) {
        if (projectPath == null) {
            return false;
        }

        try {
            if (getExercise(projectPath) == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    
    
    /**
     * Method tries to recognize project from it's path.
     * Path contains all necessary information to recognize project as an exercise:     
     * /plugin default folder/course name/exercise name/
     * @param String Project path to exercise.
     * @return Exercise Recognized exercise or null.
     * @throws IOException
     * @throws JSONException
     */
    public static Exercise getExercise(String projectPath) throws IOException {

        if (projectPath == null) {
            return null;
        }

        String defaultPath = LegacyPluginSettings.getSettings().getDefaultFolder();
        if (defaultPath == null) {
            return null;
        }

        if (projectPath.indexOf(defaultPath) == -1) {
            return null;
        }

        projectPath = projectPath.substring(defaultPath.length());

        String[] folders = projectPath.split(File.separator);

        if (folders.length < 3) {
            return null;
        }


        String courseName = folders[1];
        String exerciseName = folders[2];


        CourseCollection cc = LocalCourseCache.getInstance().getAvailableCourses();
        if (cc == null) {
            return null;
        }

        Course course = cc.getCourseByName(courseName);

        if (course != null) {
            ExerciseCollection exerciseCollection = LocalCourseCache.getInstance().getExercisesForCourse(course);

            if (exerciseCollection != null) {
                return exerciseCollection.getExerciseByName(exerciseName);
            }
        }

        return null;

    }

    
    
    /**
     * Find exercise related project.
     * @param Exercise exercise
     * @return Project
     * @throws NullPointerException
     * @throws IOException 
     */
    public static Project getProject(Exercise exercise) throws NullPointerException, IOException {
        if (exercise == null) {
            throw new NullPointerException("exercise cannot be null at ProjectHandler.getProject");
        }

        File projectFolder = FolderHelper.searchNbProject(exercise);

        if (projectFolder == null) {
            throw new NullPointerException("Couldn't find project folder at ProjectHandler.getProject");
        }

        return getProject(projectFolder);

    }

    
    
    /**
     * Find project.
     * @param File projectFolder
     * @return Project
     * @throws NullPointerException
     * @throws IOException 
     */
    public static Project getProject(File projectFolder) throws NullPointerException, IOException {

        if (projectFolder == null) {
            throw new NullPointerException("projectFolder cannot be null at ProjectHandler.getProject");
        }

        if (!projectFolder.exists()) {
            throw new IllegalArgumentException("project folder doesn't exist at ProjectHandler.getProject");
        }


        FileUtil.refreshFor(projectFolder);  //Refresh the filesystem for this folder in case the folder has been deleted through the GUI.
        //For some reason NetBeans' ProjectManager listens to external deletions but not internal ones


        FileObject projectToBeOpened = FileUtil.toFileObject(FileUtil.normalizeFile(projectFolder));
        Project project = null;

        try {

            project = ProjectManager.getDefault().findProject(projectToBeOpened);

        } catch (IOException e) {

            String msg = "Couldn't find project: " + projectFolder.getAbsolutePath() + "\r\n";
            msg += "Error msg: " + e.getMessage();

            throw new IOException(msg);
        }

        return project;
    }

    
    
    /**
     * Open project.
     * @param Project project 
     */
    public static void openProject(Project project) {
        if (project == null) {
            throw new NullPointerException("project is null at ProjectHandler.openProject");
        }
        if (OpenProjects.getDefault().isProjectOpen(project)) {
            return;
        }

        ArrayList<Project> p = new ArrayList<Project>();
        p.add(project);

        openProjects(p);
    }

    
    
    /**
     * Open projects.
     * @param ArrayList<Project> projects 
     */
    public static void openProjects(ArrayList<Project> projects) {
        if (projects.isEmpty()) {
            return;
        }

        for (int i = 0; i < projects.size(); i++) {
            if (OpenProjects.getDefault().isProjectOpen(projects.get(i))) {
                projects.remove(i);
            }
        }

        if (projects.isEmpty()) {
            return;
        }

        Project[] projectArr = projects.toArray(new Project[0]);

        OpenProjects.getDefault().open(projectArr, false, true);

    }
}
