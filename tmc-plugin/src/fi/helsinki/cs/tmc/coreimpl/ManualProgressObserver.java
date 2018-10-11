package fi.helsinki.cs.tmc.coreimpl;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.utilities.ProgressListener;
import fi.helsinki.cs.tmc.utilities.ProgressMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManualProgressObserver extends ProgressObserver {

    private static final Logger LOG = Logger.getLogger(ManualProgressObserver.class.getName());
    private final List<ProgressListener> listeners;

    public ManualProgressObserver() {
        this.listeners = new ArrayList<>();
    }

    @Override
    public void progress(long id, String message) {
        progress(id, 0.0, message);
    }

    @Override
    public void progress(long id, Double percentDone, String message) {
        ProgressMessage msg = new ProgressMessage(message, Optional.of(percentDone));
        listeners.stream().forEach(l -> l.accept(msg));
    }

    @Override
    public void start(long l) {
        LOG.log(Level.INFO, "{0} started", l);
    }

    @Override
    public void end(long l) {
        LOG.log(Level.INFO, "{0} ended", l);
    }

    public void addListener(ProgressListener listener) {
        listeners.add(listener);
    }
}
