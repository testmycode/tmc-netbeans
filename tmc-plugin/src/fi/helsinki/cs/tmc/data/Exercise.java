package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class Exercise implements Serializable {

    private int id;
    
    private String name;
    
    private String courseName;
    
    /**
     * The URL this exercise can be downloaded from.
     */
    @SerializedName("zip_url")
    private String downloadUrl;
    
    /**
     * The URL the solution can be downloaded from (admins only).
     */
    @SerializedName("solution_zip_url")
    private String solutionDownloadUrl;
    
    /**
     * The URL where this exercise should be posted for review.
     */
    @SerializedName("return_url")
    private String returnUrl;
    
    private boolean locked;
    
    @SerializedName("deadline_description")
    private String deadlineDescription;
    
    private Date deadline;
    
    private boolean returnable;
    @SerializedName("requires_review")
    private boolean requiresReview;
    private boolean attempted;
    private boolean completed;
    private boolean reviewed;
    @SerializedName("all_review_points_given")
    private boolean allReviewPointsGiven;
    private String checksum;
    
    @SerializedName("memory_limit")
    private Integer memoryLimit;

    public Exercise() {
    }

    public Exercise(String name) {
        this(name, "unknown-course");
    }

    public Exercise(String name, String courseName) {
        this.name = name;
        this.courseName = courseName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name was null at Exercise.setName");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty at Exercise.setName");
        }
        this.name = name;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getDeadlineDescription() {
        return deadlineDescription;
    }

    public void setDeadlineDescription(String deadlineDescription) {
        this.deadlineDescription = deadlineDescription;
    }
    
    public boolean hasDeadlinePassed() {
        return hasDeadlinePassedAt(new Date());
    }

    public boolean hasDeadlinePassedAt(Date time) {
        if (time == null) {
            throw new NullPointerException("Given time was null at Exercise.isDeadlineEnded");
        }
        if (deadline != null) {
            return deadline.getTime() < time.getTime();
        } else {
            return false;
        }
    }
    
    public ExerciseKey getKey() {
        return new ExerciseKey(courseName, name);
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public void setDownloadUrl(String downloadAddress) {
        if (downloadAddress == null) {
            throw new NullPointerException("downloadAddress was null at Exercise.setDownloadAddress");
        }
        if (downloadAddress.isEmpty()) {
            throw new IllegalArgumentException("downloadAddress cannot be empty at Exercise.setDownloadAddress");
        }

        this.downloadUrl = downloadAddress;
    }

    public String getReturnUrl() {
        return this.returnUrl;
    }

    public void setSolutionDownloadUrl(String solutionDownloadUrl) {
        this.solutionDownloadUrl = solutionDownloadUrl;
    }

    public String getSolutionDownloadUrl() {
        return solutionDownloadUrl;
    }

    public void setReturnUrl(String returnAddress) {
        if (returnAddress == null) {
            throw new NullPointerException("returnAddress was null at Exercise.setReturnAddress");
        }
        if (returnAddress.isEmpty()) {
            throw new IllegalArgumentException("downloadAddress cannot be empty at Exercise.setReturnAddress");
        }
        this.returnUrl = returnAddress;
    }
    
    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }
    
    public boolean isReturnable() {
        return returnable;
    }

    public void setReturnable(boolean returnable) {
        this.returnable = returnable;
    }

    public boolean requiresReview() {
        return requiresReview;
    }

    public void setRequiresReview(boolean requiresReview) {
        this.requiresReview = requiresReview;
    }

    public boolean isAttempted() {
        return attempted;
    }

    public void setAttempted(boolean attempted) {
        this.attempted = attempted;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public boolean isAllReviewPointsGiven() {
        return allReviewPointsGiven;
    }

    public void setAllReviewPointsGiven(boolean allReviewPointsGiven) {
        this.allReviewPointsGiven = allReviewPointsGiven;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
