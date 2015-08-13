package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.awt.Image;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.apache.commons.lang3.StringUtils;
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
    private HashMap<String, Image> iconCache;

    @SuppressWarnings("LeakingThisInConstructor")
    public ExerciseIconAnnotator() {
        this.eventBus = TmcEventBus.getDefault();
        this.changeSupport = new ChangeSupport(this);
        this.courses = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.iconCache = new HashMap<String, Image>();
        
        eventBus.subscribeDependent(new TmcEventListener() {
            public void receive(CourseDb.ChangedEvent event) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateAllIcons();
                    }
                });
            }
        }, this);
    }

    @Override
    public Image annotateIcon(Project nbProject, Image origImg, boolean openedNode) {
        TmcProjectInfo project = projectMediator.wrapProject(nbProject);
        Exercise exercise = projectMediator.tryGetExerciseForProject(project, courses);
        if (exercise == null || !exercise.getCourseName().equals(courses.getCurrentCourseName())) {
            return origImg;
        }
        
        //TODO: use ImageUtilities.createDisabledImage for expired exercises.
        //Had some very weird problems with that. Try again some day.
        
        Image img = origImg;
        try {
            Image annotation = annotationIconForExericse(exercise);
            if (annotation != null) {
                img = ImageUtilities.mergeImages(img, annotation, 0, 0);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to load exercise icon annotation", e);
        }
        
        String tooltip = tooltipForExercise(exercise);
        img = ImageUtilities.assignToolTipToImage(img, tooltip);
        
        return img;
    }
    
    private Image annotationIconForExericse(Exercise exercise) throws IOException {
        String name = annotationIconNameForExercise(exercise);
        if (name != null) {
            if (!iconCache.containsKey(name)) {
                Image img = ImageIO.read(getClass().getClassLoader().getResource("fi/helsinki/cs/tmc/ui/" + name));
                iconCache.put(name, img);
            }
            return iconCache.get(name);
        } else {
            return null;
        }
    }
    
    private String annotationIconNameForExercise(Exercise exercise) {
        if (exercise.hasDeadlinePassed()) {
            return null;
        } else if (exercise.isAttempted() && exercise.isCompleted() && exercise.isAllReviewPointsGiven()) {
            return "green-project-dot.png";
        } else if (exercise.isAttempted() && exercise.isCompleted()) {
            return "yellow-project-dot.png";
        } else if (exercise.isAttempted()) {
            return "red-project-dot.png";
        } else {
            return "black-project-dot.png";
        }
    }
    
    private String tooltipForExercise(Exercise exercise) {
        List<String> parts = new ArrayList<String>();
        if (exercise.isAttempted()) {
            parts.add("exercise submitted");
            if (exercise.isCompleted()) {
                parts.add("all tests successful");
            } else {
                parts.add("all tests not completed");
            }
            if (exercise.requiresReview()) {
                if (exercise.isAllReviewPointsGiven()) {
                    parts.add("code review done");
                } else if (exercise.isReviewed()) {
                    parts.add("code review done, not accepted");
                } else {
                    parts.add("code review not yet done");
                }
            }
            
        } else {
            parts.add("exercise not yet submitted");
        }
        
        if (!exercise.isCompleted() && exercise.getDeadline() != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX");
            System.out.println("Deadline: " + exercise.getDeadline());
            parts.add("deadline: " + exercise.getDeadline());
        }
        
        return StringUtils.capitalize(StringUtils.join(parts, " - "));
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
