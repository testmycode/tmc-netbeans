package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.spyware.eventsources.WindowStatechangesEventSource;
import fi.helsinki.cs.tmc.events.TmcEvent;
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
import java.io.IOException;
import java.util.logging.Logger;

public class SpywareFacade implements SpywareSettings {

    private static final Logger log = Logger.getLogger(SpywareFacade.class.getName());

    private static SpywareFacade instance;

    public static void start() {
        if (instance != null) {
            throw new IllegalStateException("SpywareFacade.start() called twice");
        }
        instance = new SpywareFacade();
        TmcEventBus.getDefault().post(new InvokedEvent("spyware_loaded"));

    }

    public static void close() {
        if (instance != null) {
            instance.closeImpl();
            instance = null;
        }
    }

    /**
     * Allows tasks to force send snapshots. Used e.g. in Submit task, so that
     * we can make sure that all of the snapshot data has been sent to the
     * server alongside submission and not lost in cases like when user submits
     * it's last exercise while using a guest account.
     *
     * We don't want to delay closing NetBeans by then sending snapshots...
     */
    public static void sendNow() {
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                instance.sender.sendNow();
            }
        });
    }

    private TmcSettings settings;

    private EventSendBuffer sender;
    private EventReceiver taggingSender;

    private EventDeduplicater sourceSnapshotDedup;

    private SourceSnapshotEventSource sourceSnapshotSource;
    private ProjectActionEventSource projectActionSource;
    private TmcEventBusEventSource tmcEventBusSource;
    private TextInsertEventSource textInsertEventSource;
    private WindowStatechangesEventSource windowStatechangesEventSource;

      private static final class TaggingEventReceiver implements EventReceiver {

        private final EventReceiver nextReceiver;
        private final String hostId;

        public TaggingEventReceiver(EventReceiver nextReceiver, String hostId) {
            this.nextReceiver = nextReceiver;
            this.hostId = hostId;
        }

        @Override
        public void receiveEvent(LoggableEvent event) {
            event.addMetadata("host_id", hostId);
            nextReceiver.receiveEvent(event);
        }

        @Override
        public void close() throws IOException {
            nextReceiver.close();
        }
    }


    public SpywareFacade() {
        settings = TmcSettings.getDefault();

        sender = new EventSendBuffer(this, new ServerAccess(), CourseDb.getInstance(), new EventStore());
        sender.sendNow();

        String hostId = new HostInformationGenerator().updateHostInformation(sender);
        taggingSender = new TaggingEventReceiver(sender, hostId);
        sourceSnapshotDedup = new EventDeduplicater(taggingSender);
        sourceSnapshotSource = new SourceSnapshotEventSource(this, sourceSnapshotDedup);
        sourceSnapshotSource.startListeningToFileChanges();

        projectActionSource = new ProjectActionEventSource(taggingSender);
        tmcEventBusSource = new TmcEventBusEventSource(taggingSender);

        windowStatechangesEventSource = new WindowStatechangesEventSource(taggingSender);
        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                ProjectActionCaptor.addListener(projectActionSource);
                TmcEventBus.getDefault().subscribeStrongly(tmcEventBusSource);
                textInsertEventSource = new TextInsertEventSource(taggingSender);
            }
        });
    }

    private void closeImpl() {
        // Close & flush back to front

        TmcSwingUtilities.ensureEdt(new Runnable() {
            @Override
            public void run() {
                TmcEventBus.getDefault().post(new InvokedEvent("spyware_unloaded"));
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

    public static class InvokedEvent implements TmcEvent {

        public final String message;

        public InvokedEvent(String message) {
            this.message = message;
        }
    }
}
