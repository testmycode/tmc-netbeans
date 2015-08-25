
package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.core.TmcCore;

public class TmcCoreSingleton {

    private static final TmcCore defaultInstance = new TmcCore(NbTmcSettings.getDefault());

    /**
     * Returns singleton instance of the {@link TmcCore}.
     */
    public static TmcCore getInstance() {
        return defaultInstance;
    }
}
