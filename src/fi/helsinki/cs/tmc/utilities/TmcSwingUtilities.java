package fi.helsinki.cs.tmc.utilities;

import java.awt.EventQueue;

public class TmcSwingUtilities {
    public static void ensureEdt(Runnable runnable) {
        try {
            if (EventQueue.isDispatchThread()) {
                runnable.run();
            } else {
                EventQueue.invokeAndWait(runnable);
            }
        } catch (Exception e) {
            throw ExceptionUtils.toRuntimeException(e);
        }
    }
}
