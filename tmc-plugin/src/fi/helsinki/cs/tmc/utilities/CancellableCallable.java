package fi.helsinki.cs.tmc.utilities;

import java.util.concurrent.Callable;
import org.openide.util.Cancellable;

/**
 * Combines {@link Callable} and {@link Cancellable}.
 */
public interface CancellableCallable<V> extends Callable<V>, Cancellable {
}
