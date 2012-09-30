package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Date;

public class Review {
    @SerializedName("submission_id")
    private int submissionId;
    @SerializedName("exercise_name")
    private String exerciseName;
    @SerializedName("marked_as_read")
    private boolean markedAsRead;
    @SerializedName("reviewer_name")
    private String reviewerName;
    @SerializedName("review_body")
    private String reviewBody;
    @SerializedName("points")
    private ArrayList<String> points;
    @SerializedName("points_not_awarded")
    private ArrayList<String> pointsNotAwarded;
    @SerializedName("url")
    private String url;
    @SerializedName("update_url")
    private String updateUrl;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("updated_at")
    private Date updatedAt;

    public int getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public boolean isMarkedAsRead() {
        return markedAsRead;
    }

    public void setMarkedAsRead(boolean markedAsRead) {
        this.markedAsRead = markedAsRead;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewBody() {
        return reviewBody;
    }

    public void setReviewBody(String reviewBody) {
        this.reviewBody = reviewBody;
    }

    public ArrayList<String> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<String> points) {
        this.points = points;
    }

    public ArrayList<String> getPointsNotAwarded() {
        return pointsNotAwarded;
    }

    public void setPointsNotAwarded(ArrayList<String> pointsNotAwarded) {
        this.pointsNotAwarded = pointsNotAwarded;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
}
