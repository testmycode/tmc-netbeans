package fi.helsinki.cs.tmc.coreimpl;

import fi.helsinki.cs.tmc.core.domain.ProgressObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.progress.ProgressHandle;

public class BridgingProgressObserver extends ProgressObserver {

    private static final Logger log = Logger.getLogger(BridgingProgressObserver.class.getName());

    ProgressHandle handle;

    @Override
    public void progress(long id, String progressMessage) {
        log.log(Level.INFO, "progress: {0}", progressMessage);
        if (handle != null) {
            log.log(Level.FINE, "notnull");
            handle.progress(progressMessage);
        }
    }

    @Override
    public void progress(long id, Double percentDone, String progressMessage) {
        // TODO: What is percentDone for?
        progress(id, progressMessage);
    }

    @Override
    public void start(long id) {
        // NOP
        log.log(Level.INFO, "{0} started", id);
    }

    @Override
    public void end(long id) {
        // NOP
        log.log(Level.INFO, "{0} ended", id);
    }

    public void attach(ProgressHandle progressHandle) {
        this.handle = progressHandle;
    }
}
