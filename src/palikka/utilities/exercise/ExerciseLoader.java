package palikka.utilities.exercise;

import java.io.IOException;
import java.util.ArrayList;
import org.netbeans.api.project.Project;
import palikka.data.Exercise;
import palikka.data.ExerciseCollection;
import palikka.utilities.ProjectHandler;

/**
 * This class can be used to open all exercises in an ExerciseCollection to
 * the IDE.
 * @author kkaltiai
 */
public class ExerciseLoader {

    /**
     * Used to open a single exercise (project) to the IDE
     * @param e
     * @throws IOException If fails to open an exercise
     */
    public static void open(Exercise e) throws IOException {
        try {
            Project project = ProjectHandler.getProject(e);
            ProjectHandler.openProject(project);

        } catch (Exception ex) {
            throw new IOException("Failed to open exercise " + e.getName());
        }
    }

    /**
     * Used to open all exercises in a collection.
     * @param coll
     * @throws IOException That is a collection of all the errors that occured 
     * while trying to open exercises
     */
    public static void openAll(ExerciseCollection coll) throws IOException {
        String errorMessage = "";
        ArrayList<Project> projects = new ArrayList<Project>();

        for (Exercise e : coll) {
            try {
                Project project = ProjectHandler.getProject(e);
                projects.add(project);

            } catch (Exception ex) {
                errorMessage += ex.getMessage() + '\n';
            }
        }

        ProjectHandler.openProjects(projects);

        if (!errorMessage.equals("")) {
            throw new IOException("The following things went wrong: " + errorMessage);  //return a group of error messages if any were encountered

        }
    }
}
