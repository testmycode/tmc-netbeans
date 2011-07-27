package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

/**
 * A single exercise holds various information concerning an exercise,
 * such as the name and download address.
 */
public class Exercise implements Serializable {

    private String name;
    
    private String courseName;
    
    /**
     * The URL this exercise can be downloaded from.
     */
    @SerializedName("zip_url")
    private String downloadAddress;
    
    /**
     * The URL where this exercise should be posted for review.
     */
    @SerializedName("return_address")
    private String returnAddress;
    
    private Date deadline;
    
    private ExerciseProgress progress = ExerciseProgress.NOT_DONE;

    public Exercise() {
    }

    public Exercise(String name) {
        this(name, "unknown-course");
    }

    public Exercise(String name, String courseName) {
        this.name = name;
        this.courseName = courseName;
    }

    /**
     * Checks wether this exercise has expired or not.
     */
    public boolean isDeadlineEnded(Date currentTime) {
        if (currentTime == null) {
            throw new NullPointerException("current time was null at Exercise.isDeadlineEnded");
        }
        return deadline.getTime() < currentTime.getTime();

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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getDownloadAddress() {
        return this.downloadAddress;
    }

    public void setDownloadAddress(String downloadAddress) {
        if (downloadAddress == null) {
            throw new NullPointerException("downloadAddress was null at Exercise.setDownloadAddress");
        }
        if (downloadAddress.isEmpty()) {
            throw new IllegalArgumentException("downloadAddress cannot be empty at Exercise.setDownloadAddress");
        }

        this.downloadAddress = downloadAddress;
    }

    public String getReturnAddress() {
        return this.returnAddress;
    }

    public void setReturnAddress(String returnAddress) {
        if (returnAddress == null) {
            throw new NullPointerException("returnAddress was null at Exercise.setReturnAddress");
        }
        if (returnAddress.isEmpty()) {
            throw new IllegalArgumentException("downloadAddress cannot be empty at Exercise.setReturnAddress");
        }
        this.returnAddress = returnAddress;
    }

    public ExerciseProgress getProgress() {
        return progress;
    }

    public void setProgress(ExerciseProgress progress) {
        this.progress = progress;
    }

    public Date getDeadline() {
        if (deadline == null) {
            throw new IllegalStateException("Deadline not set");
        }
        return deadline;
    }

    public void setDeadline(Date deadline) {
        if (deadline == null) {
            throw new NullPointerException("dealine was null at Exercise.setDeadline");
        }
        this.deadline = deadline;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
