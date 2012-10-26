package fi.helsinki.cs.tmc.spyware.eventsources;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.spyware.EventReceiver;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.spyware.SpywareSettings;
import fi.helsinki.cs.tmc.utilities.ActiveThreadSet;
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
        reactToChange(fe.getFile());
    }

    @Override
    public void fileDataCreated(FileEvent fe) {
        reactToChange(fe.getFile());
    }

    @Override
    public void fileChanged(FileEvent fe) {
        reactToChange(fe.getFile());
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        reactToChange(fe.getFile());
    }

    @Override
    public void fileRenamed(FileRenameEvent fre) {
        reactToChange(fre.getFile());
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fae) {
    }
    
    // I have no idea what thread FileUtil callbacks are made in,
    // so I'll go to the EDT to safely read the global state.
    private void reactToChange(final FileObject changedFile) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (closed) {
                    return;
                }
                try {
                    startSnapshotThread(changedFile);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to start snapshot thread", e);
                }
            }
        });
    }
    
    private void startSnapshotThread(FileObject changedFile) {
        if (!settings.isSpywareEnabled()) {
            return;
        }
        
        log.log(Level.FINE, "Changed file: {0}", changedFile);
        
        ProjectMediator pm = ProjectMediator.getInstance();
        TmcProjectInfo project = pm.tryGetProjectOwningFile(changedFile);
        log.log(Level.FINE, "Project: {0}", project);
        if (project != null) {
            CourseDb courseDb = CourseDb.getInstance();
            Exercise exercise = pm.tryGetExerciseForProject(project, courseDb);
            
            if (exercise != null) {
                log.log(Level.FINER, "Exercise: {0}", exercise);
                
                SnapshotThread thread = new SnapshotThread(receiver, exercise, project);
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

        private SnapshotThread(EventReceiver receiver, Exercise exercise, TmcProjectInfo projectInfo) {
            super("Source snapshot");
            this.receiver = receiver;
            this.exercise = exercise;
            this.projectInfo = projectInfo;
        }

        @Override
        public void run() {
            File projectDir = projectInfo.getProjectDirAsFile();
            RecursiveZipper zipper = new RecursiveZipper(projectDir, projectInfo.getZippingDecider());
            try {
                byte[] data = zipper.zipProjectSources();
                LoggableEvent event = new LoggableEvent(exercise, "code_snapshot", data);
                receiver.receiveEvent(event);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Error zipping project sources in: " + projectDir, ex);
            }
        }
    }
}
