package fi.helsinki.cs.tmc.model;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * A store of transient changes to a {@link Preferences} that may be
 * saved or canceled.
 */
public class PersistableSettings {
    
    private Preferences prefs;
    private HashMap<String, String> transients; // null values mean pending deletion
    
    /**
     * Creates a new PersistableSettings around what {@link NbPreferences#forModule(java.lang.Class)} returns.
     */
    public static PersistableSettings forModule(Class<?> cls) {
        return new PersistableSettings(NbPreferences.forModule(cls));
    }
    
    public PersistableSettings(Preferences prefs) {
        this.prefs = prefs;
        this.transients = new HashMap<String, String>();
    }
    
    public String get(String key, String def) {
        String value = transients.get(key);
        if (value != null) {
            return value;
        } else {
            return prefs.get(key, def);
        }
    }
    
    public String getPersisted(String key, String def) {
        return prefs.get(key, def);
    }
    
    public void put(String key, String value) {
        transients.put(key, value);
    }
    
    public void remove(String key) {
        transients.put(key, null);
    }
    
    public void saveAll() {
        for (Map.Entry<String, String> e : transients.entrySet()) {
            if (e.getValue() != null) {
                prefs.put(e.getKey(), e.getValue());
            } else {
                prefs.remove(e.getKey());
            }
        }
        
        flushPrefsOrThrow();
        transients.clear();
    }
    
    public void save(String key) {
        if (transients.containsKey(key)) {
            String value = transients.get(key);
            if (value != null) {
                prefs.put(key, value);
            } else {
                prefs.remove(key);
            }
            flushPrefsOrThrow();
            transients.remove(key);
        }
    }
    
    public void putAndSave(String key, String value) {
        put(key, value);
        save(key);
    }
    
    public void cancel() {
        transients.clear();
    }
    
    public Preferences getPreferences() {
        return prefs;
    }

    private void flushPrefsOrThrow() {
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            throw new RuntimeException("Failed to save settings", ex);
        }
    }
}
