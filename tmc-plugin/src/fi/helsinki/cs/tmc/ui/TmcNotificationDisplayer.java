package fi.helsinki.cs.tmc.ui;

import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.Icon;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;

/**
 * Wraps {@link NotificationDisplayer} with functionality for avoiding duplicates.
 */
public class TmcNotificationDisplayer {
    private static TmcNotificationDisplayer instance;

    public static TmcNotificationDisplayer getDefault() {
        if (instance == null) {
            instance = new TmcNotificationDisplayer(NotificationDisplayer.getDefault());
        }
        return instance;
    }
    
    public static SingletonToken createSingletonToken() {
        return new SingletonToken();
    }

    /**
     * Used to identify a kind of notification, of which there shouldn't be duplicates.
     */
    public static class SingletonToken {
        SingletonToken() {
        }
    }
    
    private NotificationDisplayer displayer;
    private HashMap<SingletonToken, Notification> lastSingletonNotification;
    
    private TmcNotificationDisplayer(NotificationDisplayer displayer) {
        this.displayer = displayer;
        this.lastSingletonNotification = new HashMap<TmcNotificationDisplayer.SingletonToken, Notification>();
    }
    
    public Notification notify(String title, Icon icon, String detailsText, ActionListener detailsAction) {
        return displayer.notify(title, icon, detailsText, detailsAction);
    }
    
    public Notification notify(SingletonToken token, String title, Icon icon, String detailsText, ActionListener detailsAction) {
        return notify(token, title, icon, detailsText, detailsAction, NotificationDisplayer.Priority.NORMAL);
    }
    
    public Notification notify(String title, Icon icon, String detailsText, ActionListener detailsAction, NotificationDisplayer.Priority priority) {
        return displayer.notify(title, icon, detailsText, detailsAction, priority);
    }
    
    public Notification notify(SingletonToken token, String title, Icon icon, String detailsText, ActionListener detailsAction, NotificationDisplayer.Priority priority) {
        Notification oldNotification = lastSingletonNotification.get(token);
        if (oldNotification != null) {
            oldNotification.clear();
        }
        Notification newNotification = displayer.notify(title, icon, detailsText, detailsAction, priority);
        lastSingletonNotification.put(token, newNotification);
        return newNotification;
    }
}
