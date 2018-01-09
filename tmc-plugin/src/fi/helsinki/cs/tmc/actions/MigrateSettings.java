package fi.helsinki.cs.tmc.actions;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.core.utilities.TmcServerAddressNormalizer;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.PersistableSettings;
import java.util.logging.Level;

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

    public static void tryToMigratePasswordToOAuthToken() {
        final TmcSettings settings = TmcSettingsHolder.get();
        try {
            TmcServerAddressNormalizer normalizer = new TmcServerAddressNormalizer();
            normalizer.normalize();
            TmcCore.get().authenticate(ProgressObserver.NULL_OBSERVER, settings.getPassword().get()).call();
            normalizer.selectOrganizationAndCourse();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Couldn't migrate password to OAuth token. The user will be asked to log in.");
        } finally {
            settings.setPassword(Optional.<String>absent());
        }
    }
}
