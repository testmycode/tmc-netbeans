package fi.helsinki.cs.tmc.spyware.eventsources;

import com.google.gson.Gson;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutputActionEventSource implements OutputActionCaptor.Listener {

    private static final Logger log = Logger.getLogger(OutputActionEventSource.class.getName());

    private final ProjectMediator projects;
    private final CourseDb courseDb;
    private final EventReceiver receiver;

    public OutputActionEventSource(EventReceiver receiver) {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.receiver = receiver;
    }

    @Override
    public void input(Exercise exercise, char character) {
        if (exercise != null) {
            Object data = Collections.singletonMap("char", character);
            String jsonData = new Gson().toJson(data);
            LoggableEvent event = new LoggableEvent(exercise, "output_action", jsonData.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
            log.log(Level.FINE, "Output action event {0} for {1}", new Object[]{
                character,
                exercise.getName()
            });
        }
    }

    @Override
    public void input(Exercise exercise, char[] characters) {
        if (exercise != null) {
            Object data = Collections.singletonMap("char", characters);
            String jsonData = new Gson().toJson(data);
            LoggableEvent event = new LoggableEvent(exercise, "output_action", jsonData.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
            log.log(Level.FINE, "Output action event {0} for {1}", new Object[]{
                Arrays.toString(characters),
                exercise.getName()
            });
        }
    }

    @Override
    public void input(Exercise exercise, CharBuffer characters) {
        if (exercise != null) {
            Object data = Collections.singletonMap("char", characters);
            String jsonData = new Gson().toJson(data);
            LoggableEvent event = new LoggableEvent(exercise, "output_action", jsonData.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
            log.log(Level.FINE, "Output action event {0} for {1}", new Object[]{
                characters,
                exercise.getName()
            });
        }
    }
}
