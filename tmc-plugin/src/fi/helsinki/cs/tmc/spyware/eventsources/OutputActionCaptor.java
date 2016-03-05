package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;

import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.NbEditorDocument;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * Provides tmc-spyware access to Output of netbeans and allows us to sniff all
 * keypresses read by app with it.
 */
@org.openide.util.lookup.ServiceProvider(service = org.openide.windows.IOProvider.class, position = -9999999)
public class OutputActionCaptor extends IOProvider {

    private static final Logger log = Logger.getLogger(OutputActionCaptor.class.getName());
    private static final String NAME = "tmc-output-logger";

    private static final CourseDb courseDb = CourseDb.getInstance();

    public static interface Listener {

        public void input(Exercise exercise, char character);
        public void input(Exercise exercise, char[] characters);
        public void input(Exercise exercise, CharBuffer characters);
    }

    private static final List<Listener> listeners = new ArrayList<Listener>();

    public synchronized static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public synchronized static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public InputOutput getIO(String string, boolean bln) {
        for (IOProvider provider : Lookup.getDefault().lookupAll(IOProvider.class)) {
            if (provider.getName().equals(NAME)) {
                continue;
            }

            InputOutput inputOutput = provider.getIO(string, bln);
            return new TmcInputOutputProxy(inputOutput);
        }
        log.log(Level.WARNING, "Failed to get IO. Please contact TestMyCode authors/teacher");
        throw new RuntimeException("Failed to get IO. Please contact TestMyCode authors/teacher");
    }

    /**
     * Proxies access to InputOutput from the default provider. Used as one
     * event source for spyware module.
     */
    private static final class TmcInputOutputProxy implements InputOutput {

        private final InputOutput original;
        private final Exercise exercise;

        public TmcInputOutputProxy(InputOutput original) {
            this.original = original;
            this.exercise = getExercise(getChangedFile());
        }

        @Override
        public OutputWriter getOut() {
            return original.getOut();
        }

        @Override
        public Reader getIn() {
            final Reader reader = original.getIn();
            return new TmcReader(reader);
        }

        /**
         * Proxies access to Reader, allowing us to sniff the interactions.
         */
        private final class TmcReader extends Reader {

            private final Reader original;

            public TmcReader(Reader original) {
                this.original = original;
            }

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return original.read(cbuf, off, len);
            }

            private void emit(char c) {
                for (Listener listener : listeners) {
                    listener.input(exercise, c);
                }
            }
            private void emit(CharBuffer charBuf) {
                for (Listener listener : listeners) {
                    listener.input(exercise, charBuf);
                }
            }

            private void emit(char[] charAry) {
                for (Listener listener : listeners) {
                    listener.input(exercise, charAry);
                }
            }

            @Override
            public int read() throws IOException {
                int c = original.read();
                emit((char) c);
                return c;
            }

            @Override
            public int read(CharBuffer target) throws IOException {
                int i = original.read(target);
                emit(target.duplicate());
                return i;
            }

            @Override
            public int read(char[] cbuf) throws IOException {
                int i = original.read(cbuf);
                emit(Arrays.copyOf(cbuf, cbuf.length));
                return i;
            }

            @Override
            public boolean ready() throws IOException {
                return original.ready();
            }

            @Override
            public void reset() throws IOException {
                original.reset();
            }

            @Override
            public long skip(long n) throws IOException {
                return original.skip(n);
            }

            @Override
            public void close() throws IOException {
                original.close();
            }

            @Override
            public void mark(int readAheadLimit) throws IOException {
                original.mark(readAheadLimit);
            }

            @Override
            public boolean markSupported() {
                return original.markSupported();
            }
        }

        @Override
        public OutputWriter getErr() {
            return original.getErr();
        }

        @Override
        public void closeInputOutput() {
            original.closeInputOutput();
        }

        @Override
        public boolean isClosed() {
            return original.isClosed();
        }

        @Override
        public void setOutputVisible(boolean bln) {
            original.setOutputVisible(bln);
        }

        @Override
        public void setErrVisible(boolean bln) {
            original.setErrVisible(bln);
        }

        @Override
        public void setInputVisible(boolean bln) {
            original.setInputVisible(bln);
        }

        @Override
        public void select() {
            original.select();
        }

        @Override
        public boolean isErrSeparated() {
            return original.isErrSeparated();
        }

        @Override
        public void setErrSeparated(boolean bln) {
            original.setErrSeparated(bln);
        }

        @Override
        public boolean isFocusTaken() {
            return original.isFocusTaken();
        }

        @Override
        public void setFocusTaken(boolean bln) {
            original.setFocusTaken(bln);
        }

        @Override
        @Deprecated
        public Reader flushReader() {
            return original.flushReader();
        }

        private Exercise getExercise(@NullAllowed FileObject obj) {
            if (obj == null) {
                return null;
            }
            ProjectMediator pm = ProjectMediator.getInstance();
            TmcProjectInfo project = pm.tryGetProjectOwningFile(obj);
            return pm.tryGetExerciseForProject(project, courseDb);
        }

        /**
         * Returns {@link FileObject} representing the last active file for each
         * event.
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

    @Override
    public OutputWriter getStdOut() {
        for (IOProvider provider : Lookup.getDefault().lookupAll(IOProvider.class)) {
            if (provider.getName().equals(NAME)) {
                continue;
            }
            return provider.getStdOut();

        }
        throw new RuntimeException("Failed to get StdOut. Please contact TestMyCode authors/teacher");
    }

    @Override
    public String getName() {
        return NAME;
    }
}
