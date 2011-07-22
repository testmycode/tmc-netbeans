package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import fi.helsinki.cs.tmc.Refactored;
import java.io.Serializable;
import java.util.Date;

/**
 * A single exercise holds various information concerning an exercise,
 * such as the name and download address.
 */
@Refactored
public class Exercise implements Serializable {

    private String name;
    
    private String courseName;
    
    /**
     * The URL this exercise can be downloaded from.
     */
    @SerializedName("exercise_file")
    private String downloadAddress;
    
    /**
     * The URL where this exercise should be posted for review.
     */
    @SerializedName("return_address")
    private String returnAddress;
    
    private Date deadline;

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
     * Exercise copy constructor.
     */
    @Deprecated
    public Exercise(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise was null at Exercise.Constructor");
        }

        name = exercise.name;
        downloadAddress = exercise.downloadAddress;
        returnAddress = exercise.returnAddress;
        deadline = new Date(exercise.deadline.getTime());
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

    /**
     * Overrides the inherited toString to return the name of this exercise instead.
     * @return The name of the exercise
     */
    @Override
    public String toString() {
        return name;
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
}
