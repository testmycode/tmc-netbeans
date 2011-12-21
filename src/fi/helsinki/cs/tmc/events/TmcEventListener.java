package fi.helsinki.cs.tmc.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Receives events related to the TMC plugin.
 *
 * Implement by overloading receive for different subclasses of {@link TmcEvent}.
 */
public abstract class TmcEventListener {
    private HashMap<Class<?>, Method> receiverMethods;

    public TmcEventListener() {
        receiverMethods = new HashMap<Class<?>, Method>();
        Method[] allMethods = this.getClass().getMethods();
        for (Method m : allMethods) {
            if (m.getName().equals("receive") && m.getParameterTypes().length == 1) {
                m.setAccessible(true);
                receiverMethods.put(m.getParameterTypes()[0], m);
            }
        }
    }

    public void receive(TmcEvent event) throws Throwable {
        Method m = receiverMethods.get(event.getClass());
        if (m != null) {
            try {
                m.invoke(this, event);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
