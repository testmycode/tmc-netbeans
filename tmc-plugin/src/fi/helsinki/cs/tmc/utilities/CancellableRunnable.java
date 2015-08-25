package fi.helsinki.cs.tmc.utilities;

import org.openide.util.Cancellable;

/**
 * Combines the {@link Runnable} and {@link Cancellable} interfaces.
 */
public interface CancellableRunnable extends Runnable, Cancellable {}
