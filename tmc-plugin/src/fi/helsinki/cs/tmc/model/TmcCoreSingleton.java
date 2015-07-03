
package fi.helsinki.cs.tmc.model;

import hy.tmc.core.TmcCore;

public class TmcCoreSingleton {

    private static TmcCore defaultInstance;
    
    public static TmcCore getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new TmcCore();
        }
        return defaultInstance;
    }
}
