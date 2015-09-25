/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.EventSendBuffer;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.windows.WindowManager;
import org.openide.windows.Mode;

public final class WindowStatechangesEventSource implements PropertyChangeListener {

    private static final Logger log = Logger.getLogger(WindowStatechangesEventSource.class.getName());

    private final ProjectMediator projects;
    private final CourseDb courseDb;
    private final EventReceiver receiver;

    public WindowStatechangesEventSource(EventReceiver receiver) {
        this.projects = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.receiver = receiver;
        startListening();
    }

    void startListening() {
        log.log(Level.INFO, "Attaching to listen WindowEventProperties");
        WindowManager.getDefault().addPropertyChangeListener(this);
    }

    /**
     * Receives window events. This includes at least events when active subwindow has changed (projects tab or code)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String data = JsonMaker.create()
                .add("name", evt.getPropertyName())
                .add("new_value", toStringWithObjects(evt.getNewValue()))
                .add("old_value", toStringWithObjects(evt.getOldValue()))
                .add("propagation_id", toStringWithObjects(evt.getPropagationId()))
                .toString();

        LoggableEvent event = new LoggableEvent(courseDb.getCurrentCourse(), "window_event", data.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
    }

    private String toStringWithObjects(Object object) {
        if (object instanceof Mode) {
            return ((Mode) object).getName();
        }
        log.log(Level.WARNING, "[spyware] Add support for toStringing class: {0}",
                object.getClass().getName());
        return object.toString();
    }
}