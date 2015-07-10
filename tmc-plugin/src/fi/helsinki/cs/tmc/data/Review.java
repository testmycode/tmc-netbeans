package fi.helsinki.cs.tmc.data;

import com.google.common.base.Optional;
import com.google.gson.annotations.SerializedName;
import hy.tmc.core.communication.HttpResult;
import hy.tmc.core.communication.UrlCommunicator;
import hy.tmc.core.exceptions.TmcCoreException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Review {

    @SerializedName("submission_id")
    private int submissionId;

    @SerializedName("exercise_name")
    private String exerciseName;

    private int id;

    @SerializedName("marked_as_read")
    private boolean markedAsRead;

    @SerializedName("reviewer_name")
    private String reviewerName;

    @SerializedName("review_body")
    private String reviewBody;

    private List<String> points;

    @SerializedName("points_not_awarded")
    private List<String> pointsNotAwarded;

    private String url;

    @SerializedName("update_url")
    private String updateUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public List<String> getPoints() {
        return points;
    }

    public void setPoints(List<String> points) {
        this.points = points;
    }

    public List<String> getPointsNotAwarded() {
        return pointsNotAwarded;
    }

    public void setPointsNotAwarded(List<String> pointsNotAwarded) {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void markAs(boolean read, UrlCommunicator urlCommunicator) throws IOException, TmcCoreException {
        Map<String, String> headers = addHeaders(read);
        HttpResult result = urlCommunicator.makePutRequest(putUrl(), Optional.of(headers));
        if (result.getData().contains("OK")) {
            this.markedAsRead = read;
        }
    }

    private Map<String, String> addHeaders(boolean read) {
        Map<String, String> headers = new HashMap<String, String>();
        String readUpdate = "mark_as_";
        if (read) {
            readUpdate += "read";
        } else {
            readUpdate += "unread";
        }
        headers.put(readUpdate, "1");
        return headers;
    }

    private String putUrl() {
        return this.updateUrl + ".json?api_version=7";
    }

    @Override
    public String toString() {
        return exerciseName + " reviewed by " + reviewerName + ":\n" + reviewBody + "\n"
                + this.url;
    }
}
