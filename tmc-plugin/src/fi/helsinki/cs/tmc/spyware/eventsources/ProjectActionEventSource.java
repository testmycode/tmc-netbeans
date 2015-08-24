package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;

import com.google.gson.Gson;

import org.netbeans.api.project.Project;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectActionEventSource implements ProjectActionCaptor.Listener {
    private static final Logger log = Logger.getLogger(ProjectActionEventSource.class.getName());

    private final ProjectMediator projects;
    private final CourseDb courseDb;
    private final EventReceiver receiver;

    public ProjectActionEventSource(EventReceiver receiver) {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.receiver = receiver;
    }

    @Override
    public void actionInvoked(Project project, String command) {
        TmcProjectInfo projectInfo = projects.wrapProject(project);
        Exercise ex = projects.tryGetExerciseForProject(projectInfo, courseDb);
        if (ex != null) {
            Object data = Collections.singletonMap("command", command);
            String jsonData = new Gson().toJson(data);
            LoggableEvent event = new LoggableEvent(ex, "project_action", jsonData.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
            log.log(Level.FINE, "Project action event {0} for {1}", new Object[] {
                command,
                projectInfo.getProjectName()
            });
        }
    }
}