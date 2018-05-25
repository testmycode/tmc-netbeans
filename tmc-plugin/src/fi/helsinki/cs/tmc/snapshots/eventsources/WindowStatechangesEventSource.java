package fi.helsinki.cs.tmc.snapshots.eventsources;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;

import com.google.common.base.CaseFormat;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.snapshots.EventReceiver;
import fi.helsinki.cs.tmc.snapshots.LoggableEvent;
import fi.helsinki.cs.tmc.core.utilities.JsonMaker;
import fi.helsinki.cs.tmc.utilities.TmcFileUtils;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.NbEditorDocument;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.annotations.common.NullAllowed;

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
        log.log(Level.INFO, "Attaching window listeners.");
        WindowManager.getDefault().addPropertyChangeListener(this);
        EditorRegistry.addPropertyChangeListener(this);
        WindowManager.getDefault().getMainWindow().addWindowListener(this);
    }

    /**
     * Receives and logs sub window change events. Such as opening and closing a new file and
     * changing between open files.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            FileObject changedFile = getChangedFile();

            Exercise exercise = getExercise(changedFile);
            String eventName = underscorify(evt.getPropertyName());

            LoggableEvent event;
            if (exercise != null) {
                log.log(Level.FINER, "Exercise: {0}", exercise);
                String filePath = TmcFileUtils.tryGetPathRelativeToProject(changedFile);
                String data = JsonMaker.create()
                        .add("new_value", toStringWithObjects(evt.getNewValue()))
                        .add("old_value", toStringWithObjects(evt.getOldValue()))
                        .add("file", toStringWithObjects(filePath))
                        .toString();

                event = new LoggableEvent(exercise, eventName, data.getBytes(Charset.forName("UTF-8")));
            } else {
                String data = JsonMaker.create()
                        .add("new_value", toStringWithObjects(evt.getNewValue()))
                        .add("old_value", toStringWithObjects(evt.getOldValue()))
                        .add("non_tmc_project", true)
                        .toString();
                event = new LoggableEvent(eventName, data.getBytes(Charset.forName("UTF-8")));
            }

            receiver.receiveEvent(event);
        } catch (Exception e) {
            log.log(Level.INFO, "Error in window event listener: {0}", e);
        }
    }

    /**
     * Logs window events.
     */
    private void reactToEvent(String action, WindowEvent event) {
        try {
            String data = JsonMaker.create()
                    .add("new_state", event.getNewState())
                    .add("old_state", event.getOldState())
                    .toString();
            if (courseDb != null) {
                Course course = courseDb.getCurrentCourse();
                if (course != null) {
                    receiver.receiveEvent(
                            new LoggableEvent(course, action, data.getBytes(Charset.forName("UTF-8"))));
                } else {
                    receiver.receiveEvent(
                            new LoggableEvent(action, data.getBytes(Charset.forName("UTF-8"))));
                }
            } else {
                log.log(Level.INFO, "Coursedb is null");
            }
        } catch (Exception e) {
            log.log(Level.INFO, "Error in window event listener: {0}", e);
        }
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

    private Exercise getExercise(@NullAllowed FileObject obj) {
        if (obj == null) {
            return null;
        }
        ProjectMediator pm = ProjectMediator.getInstance();
        TmcProjectInfo project = pm.tryGetProjectOwningFile(obj);
        if (project == null) {
            return null;
        }
        return pm.tryGetExerciseForProject(project, courseDb);
    }

    private String toStringWithObjects(Object object) {
        try {
            if (object == null) {
                return "null";
            } else if (object instanceof Mode) {
                return ((Mode) object).getName();
            } else if (object instanceof Accessible) {
                // Workaround a weird bug
                if (object instanceof JTextComponent) {
                    try {
                        ((JTextComponent) object).getCaret();
                    } catch (NullPointerException e) {
                        log.log(Level.WARNING, "JTextcomponent's caret mysteriously missing: {}", e);
                        return "null";
                    }
                }
                try {
                    Accessible acc = (Accessible) object;
                    AccessibleContext context = acc.getAccessibleContext();
                    if (context != null) {
                        String str = context.getAccessibleName();
                        if (str != null) {
                            return str;
                        }
                    }
                } catch (Exception e) {
                    log.log(Level.INFO, "Windowstate exception error: {0}", e);
                    return "accessible_exception";
                }
            }
            return object.toString();
        } catch (Exception e) {
            log.log(Level.INFO, "Error in window state change event source listener:", e);
            return "error";
        }
    }

    private String underscorify(String string) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, string);
    }

    /**
     * Returns {@link FileObject} representing the last active file for each event.
     */
    private FileObject getChangedFile() {
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc != null) {
            Document d = jtc.getDocument();
            Object fileObj = d.getProperty(NbEditorDocument.StreamDescriptionProperty);
            if (fileObj instanceof DataObject) {
                DataObject changedDataObject = (DataObject) fileObj;
                return changedDataObject.getPrimaryFile();
            }
        }
        return null;
    }
}
