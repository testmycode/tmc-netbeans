package fi.helsinki.cs.tmc.spyware.eventsources;

import com.google.gson.JsonObject;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import fi.helsinki.cs.tmc.utilities.TmcFileUtils;
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
import org.openide.util.Lookup;
import org.openide.util.datatransfer.ExClipboard;

/**
 * Records large inserts into documents. These are often, but not always,
 * pastes.
 */
public class TextInsertEventSource implements Closeable {
    
    private static final Logger log = Logger.getLogger(TextInsertEventSource.class.getName());
    private static final diff_match_patch PATCH_GENERATOR = new diff_match_patch();
    private EventReceiver receiver;
    private JTextComponent currentComponent;
    private Map<Document, String> documentCache;
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

            createAndSendPatchEvent(e, fo, doc, ex);
        }

        private void createAndSendPatchEvent(DocumentEvent e, FileObject fo, Document doc, Exercise ex) {
            List<Patch> patches;
            
            // if the document is not in cache, the patch will
            // contain the full document
            boolean patchContainsFullDocument = !documentCache.containsKey(doc);

            try {
                // generatePatches will cache the current version for future
                // patches; if the document was not in the cache previously, the patch will
                // contain the full document content
                patches = generatePatches(doc);
            } catch (BadLocationException exp) {
                log.log(Level.WARNING, "Unable to generate patches from {0}.", fo.getName());
                return;
            }

            // Remove event is handled here as the getText-method can cause
            // an error as the document state is the state after the event. As 
            // the offsets are from the actual event, they may reference content 
            // that is no longer in the current document.
            if (e.getType().equals(EventType.REMOVE)) {
                sendEvent(ex, "text_remove", generatePatchDescription(fo, patches, patchContainsFullDocument));
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
                sendEvent(ex, "text_paste", generatePatchDescription(fo, patches, patchContainsFullDocument));
            } else if (e.getType() == EventType.INSERT) {
                sendEvent(ex, "text_insert", generatePatchDescription(fo, patches, patchContainsFullDocument));
            }
        }

        private void sendEvent(Exercise ex, String eventType, String text) {
            LoggableEvent event = new LoggableEvent(ex, eventType, text.getBytes(Charset.forName("UTF-8")));
            receiver.receiveEvent(event);
        }
        
        private String generatePatchDescription(FileObject fo, List<Patch> patches, boolean patchContainsFullDocument) {
            return JsonMaker.create()
                    .add("file", TmcFileUtils.getPathRelativeToProject(fo))
                    .add("patches", PATCH_GENERATOR.patch_toText(patches))
                    .add("full_document", patchContainsFullDocument)
                    .toString();
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
        private List<Patch> generatePatches(Document doc) throws BadLocationException {
            String previous = "";
            if (documentCache.containsKey(doc)) {
                previous = documentCache.get(doc);
            }

            documentCache.put(doc, doc.getText(0, doc.getLength()));
            String current = documentCache.get(doc);

            return PATCH_GENERATOR.patch_make(previous, current);
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
        this.documentCache = new HashMap<Document, String>();
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
