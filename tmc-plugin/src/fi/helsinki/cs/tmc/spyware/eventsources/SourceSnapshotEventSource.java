package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.spyware.SpywareSettings;
import fi.helsinki.cs.tmc.utilities.ActiveThreadSet;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import fi.helsinki.cs.tmc.utilities.TmcFileUtils;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.filesystems.*;

public class SourceSnapshotEventSource implements FileChangeListener, Closeable {
    private enum ChangeType {
        FILE_CREATE, FOLDER_CREATE, FILE_CHANGE, FILE_DELETE, FILE_RENAME;
    }
    
    private static final Logger log = Logger.getLogger(SourceSnapshotEventSource.class.getName());
    
    private SpywareSettings settings;
    private EventReceiver receiver;
    private ActiveThreadSet snapshotterThreads;
    private boolean closed;

    public SourceSnapshotEventSource(SpywareSettings settings, EventReceiver receiver) {
        this.settings = settings;
        this.receiver = receiver;
        
        this.snapshotterThreads = new ActiveThreadSet();
    }
    
    public void startListeningToFileChanges() {
        FileUtil.addFileChangeListener(this);
    }
    
    /**
     * Waits for all pending events to be sent.
     */
    @Override
    public void close() {
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                try {
                    closed = true;
                    FileUtil.removeFileChangeListener(SourceSnapshotEventSource.this);
                    snapshotterThreads.joinAll();
                } catch (InterruptedException ex) {
                }
            }
        });
    }
    
    @Override
    public void fileFolderCreated(FileEvent fe) {
        reactToChange(ChangeType.FOLDER_CREATE, fe.getFile());
    }

    @Override
    public void fileDataCreated(FileEvent fe) {
        reactToChange(ChangeType.FILE_CREATE, fe.getFile());
    }

    @Override
    public void fileChanged(FileEvent fe) {
        reactToChange(ChangeType.FILE_CHANGE, fe.getFile());
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        reactToChange(ChangeType.FILE_DELETE, fe.getFile());
    }

    @Override
    public void fileRenamed(FileRenameEvent fre) {
        reactToRename(ChangeType.FILE_RENAME, fre);
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fae) {
    }
    
    private void reactToChange(final ChangeType changeType, final FileObject fileObject) {
        String filePath = TmcFileUtils.tryGetPathRelativeToProject(fileObject);
        if(filePath == null) {
            return;
        }
        
        String metadata = JsonMaker.create()
                .add("cause", changeType.name().toLowerCase())
                .add("file", filePath)
                .toString();
        invokeSnapshotThreadViaEdt(fileObject, metadata);
    }    
    
    private void reactToRename(final ChangeType changeType, final FileRenameEvent renameEvent) {
        String filePath = TmcFileUtils.tryGetPathRelativeToProject(renameEvent.getFile());
        if(filePath == null) {
            return;
        }
        
        String metadata = JsonMaker.create()
                .add("cause", changeType.name().toLowerCase())
                .add("file", filePath)
                .add("previous_name", renameEvent.getName() + "." + renameEvent.getExt())
                .toString();
        invokeSnapshotThreadViaEdt(renameEvent.getFile(), metadata);
    }
    
    // I have no idea what thread FileUtil callbacks are made in,
    // so I'll go to the EDT to safely read the global state.
    private void invokeSnapshotThreadViaEdt(final FileObject fileObject, final String metadata) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (closed) {
                    return;
                }
                
                try {
                    startSnapshotThread(fileObject, metadata);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to start snapshot thread", e);
                }
            }
        });
    }
    
    private void startSnapshotThread(FileObject changedFile, String metadata) {
        if (!settings.isSpywareEnabled()) {
            return;
        }
        
        log.log(Level.FINE, "Changed file: {0}", changedFile);
        
        ProjectMediator pm = ProjectMediator.getInstance();
        TmcProjectInfo project = pm.tryGetProjectOwningFile(changedFile);
        log.log(Level.FINE, "Project: {0}", project);
        // only log TMC-projects
        if (project != null) {
            CourseDb courseDb = CourseDb.getInstance();
            Exercise exercise = pm.tryGetExerciseForProject(project, courseDb);
            
            if (exercise != null) {
                log.log(Level.FINER, "Exercise: {0}", exercise);
                
                SnapshotThread thread = new SnapshotThread(receiver, exercise, project, metadata);
                snapshotterThreads.addThread(thread);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }
    
    private static class SnapshotThread extends Thread {
        private final EventReceiver receiver;
        private final Exercise exercise;
        private final TmcProjectInfo projectInfo;
        private final String metadata;

        private SnapshotThread(EventReceiver receiver, Exercise exercise, TmcProjectInfo projectInfo, String metadata) {
            super("Source snapshot");
            this.receiver = receiver;
            this.exercise = exercise;
            this.projectInfo = projectInfo;
            this.metadata = metadata;
        }

        @Override
        public void run() {
            // Note that, being in a thread, this is inherently prone to races that modify the project.
            // For now we just accept that. Not sure if the FileObject API would allow some sort of
            // global locking of the project.
            File projectDir = projectInfo.getProjectDirAsFile();
            RecursiveZipper zipper = new RecursiveZipper(projectDir, projectInfo.getZippingDecider());
            try {
                byte[] data = zipper.zipProjectSources();
                LoggableEvent event = new LoggableEvent(exercise, "code_snapshot", data, metadata);
                receiver.receiveEvent(event);
            } catch (IOException ex) {
                // Warning might be also appropriate, but this often races with project closing
                // during integration tests, and there warning would cause a dialog to appear,
                // failing the test.
                log.log(Level.INFO, "Error zipping project sources in: " + projectDir, ex);
            }
        }
    }
}
