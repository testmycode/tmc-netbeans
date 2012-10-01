package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Review;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Stores and periodically updates reviews.
 * 
 * <p>
 * Currently this is an in-memory-only store, so it's always initially empty when the program starts.
 */
public class ReviewDb {

    public static class NewUnreadReviewEvent implements TmcEvent {
        public final Review review;
        public NewUnreadReviewEvent(Review review) {
            this.review = review;
        }
    }
    
    public static final Logger logger = Logger.getLogger(CourseDb.class.getName());
    private static ReviewDb instance;
    
    private TmcEventBus eventBus; //TODO: send out an event on new unread reviews
    
    public static ReviewDb getInstance() {
        if (instance == null) {
            instance = new ReviewDb();
        }
        return instance;
    }
    
    
    private ArrayList<Review> reviews;
    private HashSet<Integer> reviewIdsNotifiedAbout;
    
    private ReviewDb() {
        this(TmcEventBus.getDefault());
        this.reviews = new ArrayList<Review>();
        this.reviewIdsNotifiedAbout = new HashSet<Integer>();
    }

    public ReviewDb(TmcEventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Updates the review store and fires an event if there is a new unread review.
     */
    public void setReviews(List<Review> newReviews) {
        for (Review review : newReviews) {
            if (!review.isMarkedAsRead() && !reviewIdsNotifiedAbout.contains(review.getId())) {
                notifyAboutNewReview(review);
            }
        }
        
        this.reviews.clear();
        this.reviews.addAll(newReviews);
    }
    
    /**
     * Makes it so that all unread reviews cause a notification again.
     * Normally an unread review is not notified about twice.
     */
    public void forgetReviewsNotifiedAbout() {
        reviewIdsNotifiedAbout.clear();
    }
    
    private void notifyAboutNewReview(Review review) {
        reviewIdsNotifiedAbout.add(review.getId());
        eventBus.post(new NewUnreadReviewEvent(review));
    }
}
