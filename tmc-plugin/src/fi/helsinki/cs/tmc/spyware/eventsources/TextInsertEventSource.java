package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import java.awt.HeadlessException;
import java.awt.datatransfer.DataFlavor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.datatransfer.ExClipboard;

/**
 * Records large inserts into documents. These are often, but not always,
 * pastes.
 *
 * <p>
 * NOTE: Unfortunately we currently can't distinguish autocompletes from normal
 * pastes.
 */
public class TextInsertEventSource implements Closeable {

    private static final Logger log = Logger.getLogger(TextInsertEventSource.class.getName());
    private static final diff_match_patch PATCH_GENERATOR = new diff_match_patch();
    private EventReceiver receiver;
    private JTextComponent currentComponent;
    private Map<Document, String> documentContent;
    private DocumentListener docListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            handleEvent(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handleEvent(e);
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

        private void handleEvent(DocumentEvent e) {
            Document doc = e.getDocument();
            
            FileObject fo = NbEditorUtilities.getFileObject(doc);
            if (fo == null) {
                log.log(Level.FINER, "Document has no associated file object");
                return;
            }

            Exercise ex = exerciseContainingFile(fo);
            if (ex == null) {
                log.log(Level.FINER, "Unable to determine exercise for document");
                return;
            }
            
            List<Patch> patches = generatePatches(doc);
            
            if (e.getType().equals(EventType.REMOVE)) {
                sendEvent(ex, "text_remove", generateEventDescription(fo, patches));
                return;
            }
            
            String text;
            try {
                text = doc.getText(e.getOffset(), e.getLength());
            } catch (BadLocationException exp) {
                log.log(Level.WARNING, "Document {0} event with bad location. ", e.getType());
                return;
            }

            if (isPasteEvent(text)) {
                sendEvent(ex, "text_paste", generateEventDescription(fo, patches));
            } else if (e.getType() == EventType.INSERT) {
                sendEvent(ex, "text_insert", generateEventDescription(fo, patches));
            }
        }

        private void sendEvent(Exercise ex, String eventType, String text) {
            LoggableEvent event = new LoggableEvent(ex, eventType, text.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
        }
        
        private String generateEventDescription(FileObject fo, List<Patch> patches) {
            return "{file:\"" + fo.getName() + "\", patches: \"" + PATCH_GENERATOR.patch_toText(patches) + "\"}";
        }

        private boolean isPasteEvent(String text) throws HeadlessException {
            if (text.length() <= 2 || isWhiteSpace(text)) {
                // if a short text or whitespace is inserted,
                // we skip checking for paste
                return false;
            }

            try {
                String clipboardData = (String) Lookup.getDefault().
                        lookup(ExClipboard.class).getData(DataFlavor.stringFlavor);
                return text.equals(clipboardData);
            } catch (Exception exp) {
            }
            
            return false;
        }

        private boolean isWhiteSpace(String text) {
            // If an insert is just whitespace, it's probably an autoindent

            for (int i = 0; i < text.length(); ++i) {
                if (!Character.isWhitespace(text.charAt(i))) {
                    return false;
                }
            }

            return true;
        }

        // currently, if a document is not existing, the patch will
        // contain the full file
        private List<Patch> generatePatches(Document doc) {
            String previous = "";
            if(documentContent.containsKey(doc)) {
                previous = documentContent.get(doc);
            }
            
            storeDocumentContent(doc);
            String current = documentContent.get(doc);

            return PATCH_GENERATOR.patch_make(previous, current);
        }
        
        private void storeDocumentContent(Document doc) {           
            try {
                documentContent.put(doc, doc.getText(0, doc.getLength()));
            } catch (BadLocationException ex) {
                // as we store the full document content, this should not happen
            }
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
        this.documentContent = new HashMap<Document, String>();
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
