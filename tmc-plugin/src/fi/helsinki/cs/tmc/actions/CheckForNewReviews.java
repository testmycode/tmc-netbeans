package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Review;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.PushEventListener;
import fi.helsinki.cs.tmc.model.ReviewDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.CodeReviewDialog;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForNewReviews implements ActionListener {
    private static final Logger log = Logger.getLogger(CheckForNewReviews.class.getName());
    
    private static CheckForNewReviews instance;
    
    public static void start() {
        if (instance == null) {
            instance = new CheckForNewReviews(true);
            int interval = 20*60*1000; // 20 minutes
            javax.swing.Timer timer = new javax.swing.Timer(interval, instance);
            timer.setRepeats(true);
            timer.start();
            instance.run();
        } else {
            log.warning("CheckForNewReviews.start() called twice");
        }
    }
    
    private TmcEventBus eventBus;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ReviewDb reviewDb;
    private NotificationDisplayer notifier;
    private ConvenientDialogDisplayer dialogs;
    private boolean beQuiet;
    
    CheckForNewReviews() {
        this(false);
    }

    CheckForNewReviews(boolean beQuiet) {
        this.eventBus = TmcEventBus.getDefault();
        this.serverAccess = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.reviewDb = ReviewDb.getInstance();
        this.notifier = NotificationDisplayer.getDefault();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.beQuiet = beQuiet;
        
        eventBus.subscribe(eventListener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
    
    public void run() {
        Course course = courseDb.getCurrentCourse();
        if (course == null) {
            reportError("Please select a course in TMC->Settings");
            return;
        }
        
        BgTask.start("Checking for code reviews", serverAccess.getDownloadingReviewListTask(course), new BgTaskListener<List<Review>>() {
            @Override
            public void bgTaskReady(List<Review> result) {
                reviewDb.setReviews(result);
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                reportError("Failed to check for code reviews.", ex);
            }

            @Override
            public void bgTaskCancelled() {
            }
        });
    }
    
    private TmcEventListener eventListener = new TmcEventListener() {
        public void receive(PushEventListener.ReviewAvailableEvent e) throws Throwable {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    CheckForNewReviews.this.run();
                    
                    // Exercise properties have probably changed
                    RefreshCoursesAction refresher = new RefreshCoursesAction();
                    refresher.addDefaultListener(false, true);
                    refresher.run();
                }
            });
        }

        public void receive(ReviewDb.NewUnreadReviewEvent e) throws Throwable {
            final Review review = e.review;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String title = "Code review";
                    String msg = "Code review for " + review.getExerciseName() + " ready.";
                    Image img;
                    try {
                        img = ImageIO.read(getClass().getClassLoader().getResource("fi/helsinki/cs/tmc/ui/code-review.png"));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    Icon icon = ImageUtilities.image2Icon(img);
                    
                    notifier.notify(title, icon, msg, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showReviewDialog(review);
                        }
                    });
                }
            });
        }
    };
    
    private void showReviewDialog(final Review review) {
        final CodeReviewDialog dialog = new CodeReviewDialog(review);
        dialog.setOkListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dialog.getMarkAsRead()) {
                    log.fine("Marking review as read");
                    markAsRead(review);
                }
            }
        });
        dialog.setVisible(true);
    }
    
    private void markAsRead(Review review) {
        CancellableCallable<Void> task = serverAccess.getMarkingReviewAsReadTask(review, true);
        BgTask.start("Marking review as read", task, new BgTaskListener<Void>() {
            @Override
            public void bgTaskReady(Void result) {
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                reportError("Failed to mark review as read.", ex);
            }
        });
    }

    private void reportError(final String msg) {
        reportError(msg, null);
    }
    
    private void reportError(final String msg, final Throwable ex) {
        if (ex != null) {
            log.log(Level.INFO, msg, ex);
        } else {
            log.log(Level.INFO, msg);
        }
        
        if (!beQuiet) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (ex != null) {
                        dialogs.displayError(msg, ex);
                    } else {
                        dialogs.displayError(msg);
                    }
                }
            });
        }
    }
}
