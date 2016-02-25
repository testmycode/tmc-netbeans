package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.NullAllowed;

public class LoggableEvent implements TmcEvent {

    private static final Logger log = Logger.getLogger(LoggableEvent.class.getName());

    private static int GLOBAL_HOST_ID;

    private String courseName;
    private String exerciseName;
    private String eventType;
    private int hostId;

    private byte[] data;

    @NullAllowed
    private String metadata;
    private long happenedAt; // millis from epoch
    private long systemNanotime;
    private transient String key;

    public LoggableEvent(String eventType, byte[] data) {
        this("", "", eventType, data, null);
    }

    public LoggableEvent(Exercise exercise, String eventType, byte[] data) {
        this(exercise, eventType, data, null);
    }

    public LoggableEvent(Course course, String eventType, byte[] data) {
        this(course.getName(), "", eventType, data, null);
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
        this.happenedAt = System.currentTimeMillis();
        this.systemNanotime = System.nanoTime();

        this.hostId = LoggableEvent.GLOBAL_HOST_ID;
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

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }

    public static void setGlobalHostId(int globalHostId) {
        LoggableEvent.GLOBAL_HOST_ID = globalHostId;
    }

    public static int getGlobalHostId() {
        return GLOBAL_HOST_ID;
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

    public long getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(long happenedAt) {
        this.happenedAt = happenedAt;
    }

    public long getSystemNanotime() {
        return systemNanotime;
    }




    @Override
    public String toString() {
        return "LoggableEvent{" + "courseName=" + courseName + ", exerciseName=" + exerciseName + ", eventType=" + eventType + ", happenedAt=" + happenedAt + ", systemNanotime=" + systemNanotime + ", key=" + key + ", metadata=" + metadata + ", data=" + new String(data) + "}";
    }

    /**
     * Generates information which should mostly be static throughout netbeans
     * session. However, the ip address sure could change.
     */
    private static String getStaticHostInformation() {
        JsonMaker builder = JsonMaker.create();

        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            builder.add("hostAddress", localMachine.getHostAddress());
            builder.add("hostName", localMachine.getHostName());
        } catch (UnknownHostException ex) {
            log.log(Level.WARNING, "Exception while getting host name information: {0}", ex);
        }

        try {
            Enumeration<NetworkInterface> iterator = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<String>(2);
            while (iterator.hasMoreElements()) {
                NetworkInterface networkInterface = iterator.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    macs.add(sb.toString());
                }

            }
            builder.add("mac", macs);

        } catch (SocketException ex) {
            log.log(Level.WARNING, "Exception while getting host mac information: {0}", ex);
        }

        builder.add("hostUsername", System.getProperty("user.name"));
        return builder.toString();
    }
}
