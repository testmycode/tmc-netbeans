package fi.helsinki.cs.tmc.snapshots.eventsources;

import com.google.gson.Gson;

import fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates;
import fi.helsinki.cs.tmc.actions.DownloadCompletedExercises;
import fi.helsinki.cs.tmc.actions.DownloadExercisesAction;
import fi.helsinki.cs.tmc.actions.DownloadSolutionAction;
import fi.helsinki.cs.tmc.actions.PastebinAction;
import fi.helsinki.cs.tmc.actions.SaveSettingsAction;
import fi.helsinki.cs.tmc.actions.UpdateExercisesAction;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.events.TmcEventListener;
import fi.helsinki.cs.tmc.exerciseSubmitter.ExerciseSubmitter;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.snapshots.EventReceiver;
import fi.helsinki.cs.tmc.snapshots.LoggableEvent;
import fi.helsinki.cs.tmc.snapshotsLocal.SnapshotsFacade;

import java.nio.charset.Charset;
import java.util.Collections;

/**
 * Records LoggableEvents sent over TmcEventBus, as well as some TMC action
 * invocation events.
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

    public void receive(SnapshotsFacade.InvokedEvent event) {
        sendProjectActionEvent(event.message);
    }

    public void receive(ExerciseSubmitter.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.submit");
    }

    public void receive(PastebinAction.InvokedEvent event) {
        sendProjectActionEvent(event.projectInfo, "tmc.paste");
    }

    public void receive(DownloadExercisesAction.InvokedEvent event) {
        sendProjectActionEvent(event.exercise, "tmc.download_exercise");
    }

    public void receive(DownloadSolutionAction.InvokedEvent event) {
        sendProjectActionEvent(event.exercise, "tmc.download_solution");
    }

    public void receive(UpdateExercisesAction.InvokedEvent event) {
        sendProjectActionEvent(event.exercise, "tmc.updated_exercise");
    }

    public void receive(CheckForNewExercisesOrUpdates.InvokedEvent event) {
        sendProjectActionEvent(event.course, "tmc.check_for_new_exercises_or_updates");
    }

    public void receive(DownloadCompletedExercises.InvokedEvent event) {
        sendProjectActionEvent(event.course, "tmc.download_old_completed_exercises");
    }

    public void receive(SaveSettingsAction.InvokedEvent event) {
        sendProjectActionEvent("tmc.settings_saved");
    }

    private void sendProjectActionEvent(String command) {
        Object data = Collections.singletonMap("command", command);
        String json = new Gson().toJson(data);
        byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));
        LoggableEvent event = new LoggableEvent("ide_action", jsonBytes);
        receiver.receiveEvent(event);
    }

    private void sendProjectActionEvent(Course course, String command) {
        Object data = Collections.singletonMap("command", command);
        String json = new Gson().toJson(data);
        byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));
        LoggableEvent event = new LoggableEvent(course, "course_action", jsonBytes);
        receiver.receiveEvent(event);
    }

    private void sendProjectActionEvent(TmcProjectInfo project, String command) {
        Exercise ex = projects.tryGetExerciseForProject(project, courseDb);
        sendProjectActionEvent(ex, command);
    }

    private void sendProjectActionEvent(Exercise ex, String command) {
        Object data = Collections.singletonMap("command", command);
        String json = new Gson().toJson(data);
        byte[] jsonBytes = json.getBytes(Charset.forName("UTF-8"));
        LoggableEvent event = new LoggableEvent(ex, "project_action", jsonBytes);
        receiver.receiveEvent(event);
    }
}
