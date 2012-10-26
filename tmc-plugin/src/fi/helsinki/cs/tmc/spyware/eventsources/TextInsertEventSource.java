package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;

/**
 * Records large inserts into documents. These are often, but not always, pastes.
 * 
 * <p>
 * NOTE: Unfortunately we currently can't distinguish autocompletes from normal pastes.
 */
public class TextInsertEventSource implements Closeable {
    private static final Logger log = Logger.getLogger(TextInsertEventSource.class.getName());
    
    private EventReceiver receiver;
    private JTextComponent currentComponent;
    
    private DocumentListener docListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            Document doc = e.getDocument();
            String text;
            try {
                text = doc.getText(e.getOffset(), e.getLength());
            } catch (BadLocationException ex) {
                log.log(Level.WARNING, "Document insert event with bad location.", ex);
                return;
            }
            
            if (!isInteresting(text)) {
                return;
            }
            
            FileObject fo = NbEditorUtilities.getFileObject(doc);
            if (fo != null) {
                Exercise ex = exerciseContainingFile(fo);
                if (ex != null) {
                    sendEvent(ex, text);
                }
            } else {
                log.log(Level.FINER, "Document has no associated file object");
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // These are attribute changes and don't interest us.
        }
        
        private FileObject cachedFile = null;
        private Exercise cachedExercise = null;
        
        private Exercise exerciseContainingFile(FileObject fo) {
            if (cachedFile == null || !cachedFile.equals(fo)) {
                cachedFile = fo;
                ProjectMediator pm = ProjectMediator.getInstance();
                TmcProjectInfo project = pm.tryGetProjectOwningFile(fo);
                if (project != null) {
                    cachedExercise = pm.tryGetExerciseForProject(project, CourseDb.getInstance());
                }
            }
            
            return cachedExercise;
        }
        
        private boolean isInteresting(String text) {
            // One character inserts are probably not pastes.
            // We skip 2 character inserts too as a safety margin.
            boolean isTooShort = text.length() < 3;
            
            // If an insert is just whitespace, it's probably an autoindent
            boolean isJustWhitespace = true;
            for (int i = 0; i < text.length(); ++i) {
                if (!Character.isWhitespace(text.charAt(i))) {
                    isJustWhitespace = false;
                    break;
                }
            }
            
            return !isTooShort && !isJustWhitespace;
        }

        private void sendEvent(Exercise ex, String text) {
            LoggableEvent event = new LoggableEvent(ex, "text_insert", text.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
        }
    };
    
    private PropertyChangeListener propListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            deregister();
            register();
        }
    };

    public TextInsertEventSource(EventReceiver receiver) {
        this.receiver = receiver;
        this.currentComponent = null;
        EditorRegistry.addPropertyChangeListener(propListener);
    }
    
    private void register() {
        currentComponent = EditorRegistry.lastFocusedComponent();
        if (currentComponent != null) {
            currentComponent.getDocument().addDocumentListener(docListener);
        }
    }
    
    private void deregister() {
        if (currentComponent != null) {
            currentComponent.getDocument().removeDocumentListener(docListener);
            currentComponent = null;
        }
    }
    
    @Override
    public void close() {
        deregister();
        EditorRegistry.removePropertyChangeListener(propListener);
    }
    
}
