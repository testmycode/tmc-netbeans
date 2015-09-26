package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.utilities.JsonMaker;

import org.openide.windows.WindowManager;
import org.openide.windows.Mode;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.accessibility.Accessible;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.filesystems.FileObject;

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
        EditorRegistry.addPropertyChangeListener(this);
        WindowManager.getDefault().getMainWindow().addWindowListener(this);
    }

    /**
     * Receives window events. This includes at least events when active
     * subwindow has changed (projects tab or code)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        Document d = null;
        FileObject fd = null;
        if (jtc != null) {
            d = jtc.getDocument();
            if (d instanceof FileObject) {
                fd = ((FileObject) d);
            }
            // use the document
        }

    String data = JsonMaker.create()
            .add("name", evt.getPropertyName())
            .add("new_value", toStringWithObjects(evt.getNewValue()))
            .add("old_value", toStringWithObjects(evt.getOldValue()))
            .add("fd", toStringWithObjects(fd))
            .add("propagation_id", toStringWithObjects(evt.getPropagationId()))
            .add("document", toStringWithObjects(d))
            .add("jtc", toStringWithObjects(jtc))
            .toString();

    LoggableEvent event = new LoggableEvent(courseDb.getCurrentCourse(), "window_event", data.getBytes(Charset.forName("UTF-8")));

    receiver.receiveEvent (event);
}

private String toStringWithObjects(Object object) {
        if (object == null) {
            return "null";
        }
        if (object instanceof FileObject){
            return ((FileObject) object).getName() + "--" + ((FileObject) object).getPath();
        }else if (object instanceof Mode) {
            return ((Mode) object).getName();
        } else if (object instanceof Accessible) {
            return ((Accessible) object).getAccessibleContext().getAccessibleName();
        } else if (object instanceof JEditorPane) {
            return ((JEditorPane) object).getAccessibleContext().getAccessibleName();
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
