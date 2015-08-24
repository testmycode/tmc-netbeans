package fi.helsinki.cs.tmc.tailoring;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Knows which {@link Tailoring} is set in <code>SelectedTailoring.properties</code>.
 *
 * @see <code>SelectedTailoring.properties.sample</code>
 */
public class SelectedTailoring {
    private static final Logger logger = Logger.getLogger(SelectedTailoring.class.getName());

    private static Tailoring tailoring;

    public static Tailoring get() {
        if (tailoring == null) {
            try {
                String bundleName = SelectedTailoring.class.getName().replace('.', '/');
                ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
                String className = bundle.getString("defaultTailoring");
                Class<?> cls = SelectedTailoring.class.getClassLoader().loadClass(className);
                tailoring = (Tailoring)cls.newInstance();
                logger.log(Level.INFO, "TMC using tailoring {0}", cls.getName());
            } catch (Exception e) {
                logger.log(Level.INFO, "TMC using default tailoring. ({0})", e.getMessage());
                tailoring = new DefaultTailoring();
            }
        }
        return tailoring;
    }
}