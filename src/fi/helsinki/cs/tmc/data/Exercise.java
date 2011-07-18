package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

/**
 * A single exercise holds various information concerning an exercise.
 * Such as the download address and name.
 * @author kkaltiai
 */
public class Exercise implements Serializable {

    private String name;
    
    /**
     * The address from where this exercise can be downloaded from.
     */
    @SerializedName("exercise_file")
    private String downloadAddress;
    
    /**
     * The address to where this exercise should be returned for review.
     */
    @SerializedName("return_address")
    private String returnAddress;
    private Date deadline;
    private Course course;

    public Exercise() {
    }

    /**
     * Exercise copy constructor.
     */
    public Exercise(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise was null at Exercise.Constructor");
        }

        name = exercise.name;
        downloadAddress = exercise.downloadAddress;
        returnAddress = exercise.returnAddress;
        deadline = new Date(exercise.deadline.getTime());
        course = exercise.course;
    }

    /**
     * Checks wether this exercise is expired or not.
     * @param currentTime
     * @return true if we have passed the deadline and false if not.
     */
    public boolean isDeadlineEnded(Date currentTime) {
        if (currentTime == null) {
            throw new NullPointerException("current time was null at Exercise.isDeadlineEnded");
        }
        return deadline.getTime() < currentTime.getTime();

    }

    /**
     * Method returns the name of the Exercise
     * @return this.name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Method sets the name of the Exercise
     * @param name 
     */
    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name was null at Exercise.setName");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty at Exercise.setName");
        }
        this.name = name;
    }

    /**
     * Method returns Exercise's download address
     * @return this.downloadAddress
     */
    public String getDownloadAddress() {
        return this.downloadAddress;
    }

    /**
     * Method sets Exercise's download address
     * @param downloadAddress 
     */
    public void setDownloadAddress(String downloadAddress) {
        if (downloadAddress == null) {
            throw new NullPointerException("downloadAddress was null at Exercise.setDownloadAddress");
        }
        if (downloadAddress.isEmpty()) {
            throw new IllegalArgumentException("downloadAddress cannot be empty at Exercise.setDownloadAddress");
        }

        this.downloadAddress = downloadAddress;
    }

    /**
     * Method returns Exercise's return address
     * @return this.returnAddress
     */
    public String getReturnAddress() {
        return this.returnAddress;
    }

    /**
     * Method sets Exercise's return address
     * @param returnAddress 
     */
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

    /**
     * @return the deadline
     */
    public Date getDeadline() {
        if (deadline == null) {
            throw new IllegalStateException("Deadline not set");
        }
        return deadline;
    }

    /**
     * @param deadline the deadline to set
     */
    public void setDeadline(Date deadline) {
        if (deadline == null) {
            throw new NullPointerException("dealine was null at Exercise.setDeadline");
        }

        this.deadline = deadline;
    }

    /**
     * @return the course
     */
    public Course getCourse() {
        return course;
    }

    /**
     * @param course the course to set
     */
    public void setCourse(Course course) {
        if (course == null) {
            throw new NullPointerException("course was null at Exercise.setCourse");
        }

        this.course = course;
    }
}
