package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.data.Exercise;
import java.util.Date;
import org.netbeans.api.annotations.common.NullAllowed;

public class LoggableEvent {

    private String courseName;
    private String exerciseName;
    private String eventType;
    private byte[] data;
    @NullAllowed private String metadata;
    private Date happenedAt;
    private long systemNanotime;
    private transient String key;

    public LoggableEvent(Exercise exercise, String eventType, byte[] data) {
        this(exercise, eventType, data, null);
    }

    public LoggableEvent(Exercise exercise, String eventType, byte[] data, String metadata) {
        this(exercise.getCourseName(), exercise.getName(), eventType, data, metadata);
    }

    public LoggableEvent(String courseName, String exerciseName, String eventType, byte[] data) {
        this(courseName, exerciseName, eventType, data, null);
    }

    public LoggableEvent(String courseName, String exerciseName, String eventType, byte[] data, String metadata) {
        this.courseName = courseName;
        this.exerciseName = exerciseName;
        this.eventType = eventType;
        this.data = data;
        this.metadata = metadata;
        this.happenedAt = new Date();
        this.systemNanotime = System.nanoTime();

        this.key = courseName + "|" + exerciseName + "|" + eventType;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public String getEventType() {
        return eventType;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * Optional JSON metadata.
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * {@code key = course name + "|" + exercise name + "|" + event type}
     */
    public String getKey() {
        return key;
    }

    public Date getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(Date happenedAt) {
        this.happenedAt = happenedAt;
    }

    public long getSystemNanotime() {
        return systemNanotime;
    }

    @Override
    public String toString() {
        return "LoggableEvent{" + "courseName=" + courseName + ", exerciseName=" + exerciseName + ", eventType=" + eventType + ", happenedAt=" + happenedAt + ", systemNanotime=" + systemNanotime + ", key=" + key + ", metadata=" + metadata + ", data=" + new String(data) + "}";
    }
}
