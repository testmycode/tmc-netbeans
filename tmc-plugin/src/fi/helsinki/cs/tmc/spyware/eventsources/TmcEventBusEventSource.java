package fi.helsinki.cs.tmc.spyware.eventsources;

import com.google.gson.Gson;
import fi.helsinki.cs.tmc.actions.RunTestsLocallyAction;
import fi.helsinki.cs.tmc.actions.SubmitExerciseAction;
import fi.helsinki.cs.tmc.actions.testRunner.TestRunHandler;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.Collections;

/**
 * Records TMC action invokations, such as exercise submissions.
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
    
    
    public void receive(TestRunHandler.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.test");
    }
    
    public void receive(SubmitExerciseAction.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.submit");
    }
    
    private void sendProjectActionEvent(TmcProjectInfo project, String command) {
        Exercise ex = projects.tryGetExerciseForProject(project, courseDb);
        if (ex != null) {
            Object data = Collections.singletonMap("command", command);
            String jsonData = new Gson().toJson(data);
            LoggableEvent event = new LoggableEvent(ex, "project_action", jsonData.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
        }
    }
}
