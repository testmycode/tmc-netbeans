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
    
    private ReviewDb() {
        this(TmcEventBus.getDefault());
        this.reviews = new ArrayList<Review>();
    }

    public ReviewDb(TmcEventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Updates the review store and fires an event if there is a new unread review.
     */
    public void setReviews(List<Review> newReviews) {
        Set<Integer> submissionIdsOfReviews = getSubmissionIdsOfReviews();
        
        for (Review review : newReviews) {
            if (!review.isMarkedAsRead() && !submissionIdsOfReviews.contains(review.getSubmissionId())) {
                notifyAboutNewReview(review);
            }
        }
        
        this.reviews.clear();
        this.reviews.addAll(newReviews);
    }
    
    private Set<Integer> getSubmissionIdsOfReviews() {
        Set<Integer> result = new HashSet<Integer>();
        for (Review review : reviews) {
            result.add(review.getSubmissionId());
        }
        return result;
    }

    private void notifyAboutNewReview(Review review) {
        eventBus.post(new NewUnreadReviewEvent(review));
    }
}
