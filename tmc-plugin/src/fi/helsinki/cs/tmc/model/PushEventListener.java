package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.events.TmcEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.transport.ClientTransport;
import org.cometd.websocket.client.WebSocketTransport;

/**
 * Receives HTTP push events and fires the appropriate events.
 */
public class PushEventListener {
    private static final Logger log = Logger.getLogger(PushEventListener.class.getName());
    private static final long CONNECTION_CHECK_INTERVAL = 120*1000;

    public static class ReviewAvailableEvent implements TmcEvent {
        public final String exerciseName;
        public final String url;
        public ReviewAvailableEvent(String exerciseName, String url) {
            this.exerciseName = exerciseName;
            this.url = url;
        }
    }
    
    private static PushEventListener instance;
    public static void start() {
        if (instance == null) {
            instance = new PushEventListener();
        } else {
            log.warning("PushEventListener.start() was called more than once");
        }
    }
    
    private TmcSettings settings;
    private CourseDb courseDb;
    private TmcEventBus eventBus;
    private BayeuxClient client;
    private boolean shouldReconnect;

    PushEventListener() {
        this.settings = TmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.eventBus = TmcEventBus.getDefault();
        this.shouldReconnect = false;
        
        initClientIfPossible();
        
        this.eventBus.subscribeDependent(new TmcEventListener() {
            public void receive(TmcSettings.SavedEvent e) {
                reconnect();
            }
            
            public void receive(CourseDb.ChangedEvent e) {
                reconnect();
            }
        }, this);
        
        java.util.Timer timer = new java.util.Timer("PushEventListener reconnect", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ensureConnected();
            }
        }, CONNECTION_CHECK_INTERVAL);
    }
    
    private synchronized void ensureConnected() {
        if (client != null && client.isDisconnected()) {
            initClientIfPossible();
        }
    }
    
    private synchronized void reconnect() {
        if (client != null && client.isConnected()) {
            shouldReconnect = true;
            client.disconnect();
        } else {
            initClientIfPossible();
        }
    }
    
    private synchronized void initClientIfPossible() {
        Course course = courseDb.getCurrentCourse();
        if (course == null) {
            log.fine("Not connecting to comet since no course is selected");
            return;
        }
        if (!hasEnoughSettings()) {
            log.fine("Not connecting to comet since server settings are not set");
            return;
        }
        
        String cometUrl = course.getCometUrl();
        ClientTransport transport;
        try {
            transport = createWebSocketTransport(cometUrl);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to initialize web socket transport.", ex);
            return;
        }

        client = new BayeuxClient(cometUrl, transport);
        client.getChannel(Channel.META_HANDSHAKE).addListener(handshakeListener);
        client.getChannel(Channel.META_DISCONNECT).addListener(disconnectListener);

        client.addExtension(getAuthenticationExtension(getAuthFields()));
        client.handshake();
    }
    
    private ClientTransport createWebSocketTransport(String cometUrl) throws Exception {
        Map<String, Object> transportOpts = new HashMap<String, Object>();
        WebSocketTransport.Factory factory = new WebSocketTransport.Factory();
        return factory.newClientTransport(cometUrl, transportOpts);
    }
    
    private boolean hasEnoughSettings() {
        return !"".equals(settings.getUsername()) &&
                !"".equals(settings.getPassword()) &&
                !"".equals(settings.getServerBaseUrl());
    }
    
    public ClientSession.Extension getAuthenticationExtension(final Map<String, Object> fields) {
        return new ClientSession.Extension() {
            @Override
            public boolean rcv(ClientSession session, Message.Mutable message) {
                return true;
            }

            @Override
            public boolean rcvMeta(ClientSession session, Message.Mutable message) {
                return true;
            }

            @Override
            public boolean send(ClientSession session, Message.Mutable message) {
                return true;
            }

            @Override
            public boolean sendMeta(ClientSession session, Message.Mutable message) {
                message.getExt(true).put("authentication", fields);
                return true;
            }
        };
    }
    
    private Map<String, Object> getAuthFields() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("username", settings.getUsername());
        result.put("password", settings.getPassword());
        result.put("serverBaseUrl", settings.getServerBaseUrl());
        return result;
    }
    
    private MessageListener handshakeListener = new MessageListener() {
        @Override
        public void onMessage(ClientSessionChannel csc, Message msg) {
            if (msg.isSuccessful()) {
                subscribeToReviews();
                log.fine("Comet handshake successful.");
            } else {
                log.info("Comet handshake with failed. Will retry.");
            }
        }
    };
    
    private MessageListener disconnectListener = new MessageListener() {
        @Override
        public void onMessage(ClientSessionChannel csc, Message msg) {
            if (msg.isSuccessful()) {
                handleDisconnect();
            } else {
                log.warning("WTF, received a failed comet disconnect msg.");
            }
        }
    };
    
    private synchronized void handleDisconnect() {
        if (shouldReconnect) {
            shouldReconnect = false;
            ensureConnected();
        } else {
            log.info("Comet disconnected without plans for immediate reconnect.");
        }
    }
    
    private synchronized void subscribeToReviews() {
        String username = settings.getUsername();
        String channel = "/broadcast/user/" + username + "/review-available";
        client.getChannel(channel).subscribe(reviewAvailableListener);
    }
    
    private MessageListener reviewAvailableListener = new MessageListener() {
        @Override
        public void onMessage(ClientSessionChannel csc, Message msg) {
            log.log(Level.INFO, "Comet message on review-available: {0}", msg);
            Map<String, Object> data = msg.getDataAsMap();
            eventBus.post(new ReviewAvailableEvent(
                    data.get("exercise_name").toString(),
                    data.get("url").toString()
                    ));
        }
    };
}
