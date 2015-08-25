package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.runners.TestRunHandler;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;

import com.google.gson.Gson;

import java.nio.charset.Charset;
import java.util.Collections;

/**
 * Records LoggableEvents sent over TmcEventBus, as well as some TMC action invocation events.
 */
public class TmcEventBusEventSource extends TmcEventListener {

    private ProjectMediator projects;
    private CourseDb courseDb;
    private EventReceiver receiver;

    public TmcEventBusEventSource(EventReceiver receiver) {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.receiver = receiver;
    }

    public void receive(LoggableEvent event) {
        receiver.receiveEvent(event);
    }

    public void receive(TestRunHandler.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.test");
    }

    public void receive(ExerciseSubmitter.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.submit");
    }

    private void sendProjectActionEvent(TmcProjectInfo project, String command) {
        Exercise ex = projects.tryGetExerciseForProject(project, courseDb);
        if (ex != null) {
            Object data = Collections.singletonMap("command", command);
            String json = new Gson().toJson(data);
            byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));
            LoggableEvent event = new LoggableEvent(ex, "project_action", jsonBytes);
            receiver.receiveEvent(event);
        }
    }
}