package palikka.utilities;

import java.awt.Image;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import javax.imageio.ImageIO;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectIconAnnotator;
import org.openide.util.ChangeSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import palikka.data.Exercise;
import palikka.utilities.exercise.ExerciseStatus;

/**
 * This class is registered into the Netbeans' lookup and it changes the projects' default icon if it is an exercise project.
 * @author jmturpei
 */
@ServiceProvider(service = ProjectIconAnnotator.class)
public class IconAnnotator implements ProjectIconAnnotator {

    private final ChangeSupport pcs = new ChangeSupport(this);

    /**
     * Updates icon at project name.
     * @param p Netbeans project.
     * @param orig
     * @param openedNode
     * @return Image
     */
    @Override
    public Image annotateIcon(Project p, Image orig, boolean openedNode) {

        ClassLoader syscl = getClass().getClassLoader();

        Image image;
        ExerciseStatus status;

        String projectPath = p.getProjectDirectory().getPath();
        if (!ProjectHandler.isExercise(projectPath)) {
            return orig;
        }

        try {
            Exercise exercise = ProjectHandler.getExercise(projectPath);

            DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String deadline = " Deadline " + formatter.format(exercise.getDeadline());

            status = ExerciseStatus.getStatus(exercise);
            String tooltip;

            switch (status.getStatus()) {
                case AllTestsPassed:
                    image = ImageIO.read(syscl.getResource("palikka/smiley.gif"));
                    tooltip = "Exercise was sent and all tests passed.";
                    break;

                case SendAndSomeTestsFailed:
                    image = ImageIO.read(syscl.getResource("palikka/serious.gif"));
                    tooltip = "Exercise was sent and some tests failed.";
                    break;


                case NotSend:
                default:
                    image = ImageIO.read(syscl.getResource("palikka/frownie.gif"));
                    tooltip = "This exercise hasn't been sent yet.";
                    break;
            }
            tooltip += deadline;

            return ImageUtilities.assignToolTipToImage(image, tooltip);

        } catch (Exception e) {
            //something bad happened. However we can't show any error dialogs
            //because NB decides when it calls this method.
            //error dialogs could pop up constantly.
            return orig;
        }


    }

    /**
     * Add listener.
     * @param listener 
     */
    @Override
    public void addChangeListener(ChangeListener listener) {
        pcs.addChangeListener(listener);
    }

    /**
     * Remove listener.
     * @param listener 
     */
    @Override
    public void removeChangeListener(ChangeListener listener) {
        pcs.removeChangeListener(listener);
    }

    /**
     * Start update.
     */
    private void update() {

        pcs.fireChange();
    }

    /**
     * Update icons.
     */
    public static void UpdateIcons() {
        Collection<? extends ProjectIconAnnotator> annotators = Lookup.getDefault().lookupAll(ProjectIconAnnotator.class);

        for (ProjectIconAnnotator annotator : annotators) {
            if (annotator instanceof IconAnnotator) {
                ((IconAnnotator) annotator).update();
                return;
            }
        }
    }
}
