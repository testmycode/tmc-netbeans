
package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.core.TmcCore;

public class TmcCoreSingleton {

    private static TmcCore defaultInstance;
    
    public static TmcCore getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new TmcCore();
        }
        return defaultInstance;
    }
}
