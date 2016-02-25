package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.spyware.eventsources.WindowStatechangesEventSource;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.spyware.eventsources.TextInsertEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionCaptor;
import fi.helsinki.cs.tmc.spyware.eventsources.ProjectActionEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.SourceSnapshotEventSource;
import fi.helsinki.cs.tmc.spyware.eventsources.TmcEventBusEventSource;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
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
        TmcEventBus.getDefault().post(new InvokedEvent("spyware_loaded"));

        updateHostInformation();
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

    private EventDeduplicater sourceSnapshotDedup;

    private SourceSnapshotEventSource sourceSnapshotSource;
    private ProjectActionEventSource projectActionSource;
    private TmcEventBusEventSource tmcEventBusSource;
    private TextInsertEventSource textInsertEventSource;
    private WindowStatechangesEventSource windowStatechangesEventSource;

    public SpywareFacade() {
        settings = TmcSettings.getDefault();

        sender = new EventSendBuffer(this, new ServerAccess(), CourseDb.getInstance(), new EventStore());
        sender.sendNow();

        sourceSnapshotDedup = new EventDeduplicater(sender);
        sourceSnapshotSource = new SourceSnapshotEventSource(this, sourceSnapshotDedup);
        sourceSnapshotSource.startListeningToFileChanges();

        projectActionSource = new ProjectActionEventSource(sender);
        tmcEventBusSource = new TmcEventBusEventSource(sender);

        windowStatechangesEventSource = new WindowStatechangesEventSource(sender);
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

    private static void updateHostInformation() {
        JsonMaker data = getStaticHostInformation();
        // Should be unique enough not to collapse among singe users machines.
        int hostId = data.toString().hashCode();
        LoggableEvent.setGlobalHostId(hostId);

        data.add("hostId", hostId);

        LoggableEvent event = new LoggableEvent("host_information_update", data.toString().getBytes(Charset.forName("UTF-8")));
        TmcEventBus.getDefault().post(event);
    }

    private static JsonMaker getStaticHostInformation() {
        JsonMaker builder = JsonMaker.create();

        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            builder.add("hostAddress", localMachine.getHostAddress());
            builder.add("hostName", localMachine.getHostName());
        } catch (Exception ex) {
            log.log(Level.WARNING, "Exception while getting host name information: {0}", ex);
        }

        try {
            Enumeration<NetworkInterface> iterator = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<String>(2);
            while (iterator.hasMoreElements()) {
                NetworkInterface networkInterface = iterator.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    macs.add(sb.toString());
                }
            }
            builder.add("mac_addresses", macs);

        } catch (Exception ex) {
            log.log(Level.WARNING, "Exception while getting host mac information: {0}", ex);
        }

        try {
            builder.add("user.name", System.getProperty("user.name"));
            builder.add("java.runtime.version", System.getProperty("java.runtime.version"));
            builder.add("os.name", System.getProperty("os.name"));
            builder.add("os.version", System.getProperty("os.version"));
        } catch (Exception e) {
            log.log(Level.WARNING, "Exception while getting basic host information: {0}", e);
        }

        return builder;
    }
}
