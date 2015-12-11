package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Review;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.PushEventListener;
import fi.helsinki.cs.tmc.model.ReviewDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.ui.CodeReviewDialog;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;

import com.google.gson.Gson;

import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

public class ReviewEventListener extends TmcEventListener {
    private static final Logger log = Logger.getLogger(ReviewEventListener.class.getName());

    private static final TmcNotificationDisplayer.SingletonToken notifierToken =
            TmcNotificationDisplayer.createSingletonToken();

    private static ReviewEventListener instance;

    public static void start() {
        if (instance == null) {
            instance = new ReviewEventListener();
            TmcEventBus.getDefault().subscribeStrongly(instance);
        } else {
            log.warning("ReviewEventListener.start() called twice");
        }
    }

    private ServerAccess serverAccess;
    private TmcNotificationDisplayer notifier;
    private CourseDb courseDb;
    private TmcEventBus eventBus;

    ReviewEventListener() {
        this.serverAccess = new ServerAccess();
        this.notifier = TmcNotificationDisplayer.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.eventBus = TmcEventBus.getDefault();
    }

    public void receive(PushEventListener.ReviewAvailableEvent e) throws Throwable {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        new CheckForNewReviews(true, false, false).run();
                    }
                });
    }

    public void receive(ReviewDb.NewUnreadReviewEvent e) throws Throwable {
        final Review review = e.review;
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            refreshCourseDb();
                        } catch (TmcCoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }

                        String title = "Code review";
                        String msg = "Code review for " + review.getExerciseName() + " ready.";
                        Image img;
                        try {
                            img =
                                    ImageIO.read(
                                            getClass()
                                                    .getClassLoader()
                                                    .getResource(
                                                            "fi/helsinki/cs/tmc/ui/code-review.png"));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        Icon icon = ImageUtilities.image2Icon(img);

                        notifier.notify(
                                notifierToken,
                                title,
                                icon,
                                msg,
                                new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        showReviewDialog(review);
                                    }
                                });
                    }
                });
    }

    private void refreshCourseDb() throws TmcCoreException {
        // Exercise properties have probably changed
        new RefreshCoursesAction().addDefaultListener(false, true).run();
    }

    private void showReviewDialog(final Review review) {
        final CodeReviewDialog dialog = new CodeReviewDialog(review);
        dialog.setOkListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (dialog.getMarkAsRead()) {
                            log.fine("Marking review as read");
                            markAsRead(review);

                            sendLoggableEvent(review);

                            // The review might have made new exercises available.
                            // We already updated the course DB earlier, but this time
                            // we will also notify the user.
                            new CheckForNewExercisesOrUpdates(true, false).run();
                        }
                    }
                });
        dialog.setVisible(true);
    }

    private void markAsRead(Review review) {
        CancellableCallable<Void> task = serverAccess.getMarkingReviewAsReadTask(review, true);
        BgTask.start(
                "Marking review as read",
                task,
                new BgTaskListener<Void>() {
                    @Override
                    public void bgTaskReady(Void result) {}

                    @Override
                    public void bgTaskCancelled() {}

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        log.log(Level.INFO, "Failed to mark review as read.", ex);
                    }
                });
    }

    private void sendLoggableEvent(Review review) {
        String courseName = courseDb.getCurrentCourseName();
        if (courseName != null) {
            ReviewOpened dataObject = new ReviewOpened(review);
            String json = new Gson().toJson(dataObject);
            byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));

            LoggableEvent event =
                    new LoggableEvent(
                            courseName, review.getExerciseName(), "review_opened", jsonBytes);
            eventBus.post(event);
        }
    }

    private static class ReviewOpened {
        public final int id;
        public final int submissionId;
        public final URI url;
        public final boolean markedAsRead;

        public ReviewOpened(Review review) {
            this.id = review.getId();
            this.url = review.getUrl();
            this.submissionId = review.getSubmissionId();
            this.markedAsRead = review.isMarkedAsRead();
        }
    }
}
