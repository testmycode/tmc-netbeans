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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.windows.WindowManager;
import org.openide.windows.Mode;
import org.openide.windows.WindowSystemEvent;

public final class WindowStatechangesEventSource implements PropertyChangeListener, WindowListener {

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
        WindowManager.getDefault().getMainWindow().addWindowListener(this);
    }

    /**
     * Receives window events. This includes at least events when active
     * subwindow has changed (projects tab or code)
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
        if (object == null) {
            return "null";
        }
        if (object instanceof Mode) {
            return ((Mode) object).getName();
        }
        log.log(Level.WARNING, "[spyware] Add support for toStringing class: {0}",
                object.getClass().getName());
        return object.toString();
    }

    @Override
    public void windowOpened(WindowEvent e) {
        reactToEvent("window_opened", e);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        reactToEvent("window_closing", e);
    }

    @Override
    public void windowClosed(WindowEvent e) {
        reactToEvent("window_closed", e);
    }

    @Override
    public void windowIconified(WindowEvent e) {
        reactToEvent("window_iconified", e);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        reactToEvent("window_deiconified", e);
    }

    @Override
    public void windowActivated(WindowEvent e) {
        reactToEvent("window_activated", e);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        reactToEvent("window_deactivated", e);
    }

    void reactToEvent(String action, WindowEvent event) {
        String data = JsonMaker.create()
                .add("action", action)
                .add("new_state", event.getNewState())
                .add("old_state", event.getOldState())
                .toString();
        receiver.receiveEvent(
                new LoggableEvent(courseDb.getCurrentCourse(), "window_event", data.getBytes(Charset.forName("UTF-8"))));
    }
}