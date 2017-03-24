package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.PersistableSettings;

import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

public class MigrateSettings {
    
    private static Logger logger = Logger.getLogger(MigrateSettings.class.getName());
    
    public static void run() {
        PersistableSettings rightSettings = PersistableSettings.forModule(TmcCoreSettingsImpl.class);
        PersistableSettings wrongSettings = PersistableSettings.forModule(TmcSettings.class);
        try {
            final String[] keys = wrongSettings.getPreferences().keys();
            for (String key : keys) {
                String wrongValue = wrongSettings.get(key, null);
                if (wrongValue == null) {
                    continue;
                }
                rightSettings.put(key, wrongValue);
                wrongSettings.remove(key);
            }
            wrongSettings.saveAll();
            rightSettings.saveAll();
        } catch (BackingStoreException ex) {
            logger.fine("Could not migrate wrong settings.");
        }
    }
}
