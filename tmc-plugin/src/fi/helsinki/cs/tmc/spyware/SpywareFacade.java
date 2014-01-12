package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.spyware.eventsources.TextInsertEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionCaptor;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.SourceSnapshotEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.TmcEventBusEventSource;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;
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
    
    private EventSendBuffer sender;
    
    private EventDeduplicater sourceSnapshotDedup;
    
    private SourceSnapshotEventSource sourceSnapshotSource;
    private ProjectActionEventSource projectActionSource;
    private TmcEventBusEventSource tmcEventBusSource;
    private TextInsertEventSource textInsertEventSource;
    
    public SpywareFacade() {
        settings = TmcSettings.getDefault();
        
        sender = new EventSendBuffer(this, new ServerAccess(), CourseDb.getInstance(), new EventStore());
        sender.sendNow();
        
        sourceSnapshotDedup = new EventDeduplicater(sender);
        sourceSnapshotSource = new SourceSnapshotEventSource(this, sourceSnapshotDedup);
        sourceSnapshotSource.startListeningToFileChanges();
        
        projectActionSource = new ProjectActionEventSource(sender);
        tmcEventBusSource = new TmcEventBusEventSource(sender);
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                ProjectActionCaptor.addListener(projectActionSource);
                TmcEventBus.getDefault().subscribeStrongly(tmcEventBusSource);
                textInsertEventSource = new TextInsertEventSource(sender);
            }
        });
    }
    
    private void closeImpl() {
        // Close & flush back to front
        
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                textInsertEventSource.close();
                TmcEventBus.getDefault().unsubscribe(tmcEventBusSource);
                ProjectActionCaptor.removeListener(projectActionSource);
            }
        });
        
        sourceSnapshotSource.close();
        
        sourceSnapshotDedup.close();
        sender.close();
    }
    
    @Override
    public boolean isSpywareEnabled() {
        return settings.isSpywareEnabled();
    }

    @Override
    public boolean isDetailedSpywareEnabled() {
        return settings.isDetailedSpywareEnabled();
    }
}
