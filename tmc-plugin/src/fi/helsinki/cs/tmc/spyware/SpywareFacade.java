package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionCaptor;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.SourceSnapshotEventSource;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpywareFacade implements SpywareSettings {
    private static final Logger log = Logger.getLogger(SpywareFacade.class.getName());

    private static SpywareFacade instance;
    
    public static void start() {
        if (instance != null) {
            throw new IllegalStateException("SpywareFacade.start() called twice");
        }
        instance = new SpywareFacade();
    }
    
    public static void close() {
        if (instance != null) {
            instance.closeImpl();
            instance = null;
        }
    }
    
    private TmcSettings settings;
    
    private EventStore store;
    private EventSender sender;
    
    private EventDeduplicater dedup;
    
    private SourceSnapshotEventSource sourceSnapshotSource;
    private ProjectActionEventSource projectActionSource;
    
    public SpywareFacade() {
        settings = TmcSettings.getDefault();
        
        store = new EventStore();
        sender = new EventSender();
        int loadedEventCount = loadEvents();
        if (loadedEventCount > 0) {
            sender.sendNow();
        }
        
        dedup = new EventDeduplicater(sender);
        
        sourceSnapshotSource = new SourceSnapshotEventSource(this, dedup);
        sourceSnapshotSource.startListeningToFileChanges();
        
        projectActionSource = new ProjectActionEventSource(sender);
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                ProjectActionCaptor.addListener(projectActionSource);
            }
        });
    }
    
    private int loadEvents() {
        try {
            List<LoggableEvent> events = store.load();
            store.clear();
            sender.prependEvents(events);
            return events.size();
        } catch (Exception ex) {
            log.log(Level.INFO, "Failed to load events on startup", ex);
            return 0;
        }
    }
    
    private void closeImpl() {
        // Close & flush back to front
        
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                ProjectActionCaptor.removeListener(projectActionSource);
            }
        });
        
        sourceSnapshotSource.close();
        
        dedup.close();
        sender.close();
        
        saveEvents();
    }
    
    private void saveEvents() {
        try {
            store.save(sender.takeBuffer());
        } catch (IOException ex) {
            log.log(Level.INFO, "Failed to save events on shutdown", ex);
        }
    }
    
    @Override
    public boolean isSpywareEnabled() {
        return settings.isSpywareEnabled();
    }
}
