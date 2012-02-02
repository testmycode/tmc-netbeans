package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectIconAnnotator;
import org.openide.util.ChangeSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ProjectIconAnnotator.class)
public class ExerciseIconAnnotator implements ProjectIconAnnotator {

    private static final Logger log = Logger.getLogger(ExerciseIconAnnotator.class.getName());

    private TmcEventBus eventBus;
    private ChangeSupport changeSupport;
    private CourseDb courses;
    private ProjectMediator projectMediator;

    @SuppressWarnings("LeakingThisInConstructor")
    public ExerciseIconAnnotator() {
        this.eventBus = TmcEventBus.getDefault();
        this.changeSupport = new ChangeSupport(this);
        this.courses = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        
        eventBus.subscribe(new TmcEventListener() {
            public void receive(CourseDb.SavedEvent event) {
                updateAllIcons();
            }
        });
    }

    @Override
    public Image annotateIcon(Project nbProject, Image origImg, boolean openedNode) {
        TmcProjectInfo project = projectMediator.wrapProject(nbProject);
        Exercise exercise = projectMediator.tryGetExerciseForProject(project, courses);
        if (exercise == null || !exercise.getCourseName().equals(courses.getCurrentCourseName())) {
            return origImg;
        }
        
        Image img;
        try {
            img = imageForExericse(exercise);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to load exercise icon annotation", e);
            return origImg;
        }
        
        img = ImageUtilities.mergeImages(origImg, img, 0, 0);
        
        String tooltip = tooltipForExercise(exercise);
        img = ImageUtilities.assignToolTipToImage(img, tooltip);
        
        return img;
    }
    
    private Image imageForExericse(Exercise exercise) throws IOException {
        String name = imageNameForExercise(exercise);
        return ImageIO.read(getClass().getClassLoader().getResource("fi/helsinki/cs/tmc/ui/" + name));
    }
    
    private String imageNameForExercise(Exercise exercise) {
        if (exercise.isAttempted() && exercise.isCompleted()) {
            return "green-project-dot.png";
        } else if (exercise.isAttempted()) {
            return "red-project-dot.png";
        } else {
            return "black-project-dot.png";
        }
    }
    
    private String tooltipForExercise(Exercise exercise) {
        if (exercise.isAttempted() && exercise.isCompleted()) {
            return "Exercise submitted - all tests successful";
        } else if (exercise.isAttempted()) {
            return "Exercise submitted - all tests not completed";
        } else {
            return "Exercise not yet submitted";
        }
    }
    
    public void updateAllIcons() {
        changeSupport.fireChange();
    }
    
    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }
}
