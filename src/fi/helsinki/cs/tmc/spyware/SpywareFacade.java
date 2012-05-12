package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.spyware.eventsources.SourceSnapshotEventSource;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

@ServiceProvider(service=SpywareFacade.class)
public class SpywareFacade implements SpywareSettings {
    private static final Logger log = Logger.getLogger(SpywareFacade.class.getName());
    
    private TmcSettings settings;
    
    private EventStore store;
    private EventSender sender;
    
    private EventDeduplicater dedup;
    
    private SourceSnapshotEventSource sourceSnapshotSource;
    
    public SpywareFacade() {
        settings = TmcSettings.getDefault();
        
        store = new EventStore();
        sender = new EventSender();
        loadEvents();
        
        dedup = new EventDeduplicater(sender);
        
        sourceSnapshotSource = new SourceSnapshotEventSource(this, dedup);
        sourceSnapshotSource.startListeningToFileChanges();
        
        // We can't seem to reliably catch saves done during IDE shutdown,
        // so we'll always send a snapshot of all projects with open files
        // during startup.
        snapshotProjectsWithOpenFiles();
    }
    
    private void snapshotProjectsWithOpenFiles() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                
            }
        });
    }
    
    private void loadEvents() {
        try {
            List<LoggableEvent> events = store.load();
            store.clear();
            sender.prependEvents(events);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to load events on startup", ex);
        }
    }
    
    public void close() {
        // Close & flush back to front
        sourceSnapshotSource.close();
        dedup.close();
        sender.close();
        
        if (settings.isSpywareEnabled()) {
            saveEvents();
        }
    }
    
    private void saveEvents() {
        try {
            store.save(sender.takeBuffer());
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to save events on shutdown", ex);
        }
    }
    
    @Override
    public boolean isSpywareEnabled() {
        return TmcSettings.getDefault().isSpywareEnabled();
    }
}
