package fi.helsinki.cs.tmc.utilities;

import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;

/**
 * A task that calls {@link BgTaskListener} when finished and
 * displays a progress indicator in NetBeans. It cancels by
 * sending a thread interrupt unless the given {@link Callable} is
 * also {@link Cancellable}.
 */
public class BgTask<V> implements CancellableCallable<V> {

    private RequestProcessor requestProcessor;
    private String label;
    private BgTaskListener<? super V> listener;
    private Callable<V> callable;
    private ProgressHandle progressHandle;
    private ProgressObserver proressObserver;

    private final Object cancelLock = new Object();
    private boolean cancelled;
    private Thread executingThread;

    public static <V> Future<V> start(String label, Callable<V> callable) {
        return new BgTask<V>(label, callable).start();
    }

    public static <V> Future<V> start(String label, Callable<V> callable, ProgressObserver observer, BgTaskListener<? super V> listener) {
        return new BgTask<V>(label, callable, observer, listener).start();
    }

    public static <V> Future<V> start(String label, Callable<V> callable, BgTaskListener<V> listener) {
        return new BgTask<V>(label, callable, ProgressObserver.NULL_OBSERVER, listener).start();
    }

    public static Future<Object> start(String label, Runnable runnable) {
        Callable<Object> callable = runnableToCallable(runnable);
        return start(label, callable);
    }

    public static Future<Object> start(String label, Runnable runnable, BgTaskListener<Object> listener) {
        Callable<Object> callable = runnableToCallable(runnable);
        return start(label, callable, ProgressObserver.NULL_OBSERVER, listener);
    }

    public static Future<Object> start(String label, Runnable runnable, ProgressObserver observer, BgTaskListener<Object> listener) {
        Callable<Object> callable = runnableToCallable(runnable);
        return start(label, callable, observer, listener);
    }

    private static Callable<Object> runnableToCallable(final Runnable runnable) {
        if (runnable instanceof Cancellable) {
            return new CancellableCallable<Object>() {
                @Override
                public Object call() throws Exception {
                    runnable.run();
                    return null;
                }

                @Override
                public boolean cancel() {
                    return ((Cancellable)runnable).cancel();
                }
            };
        } else {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    runnable.run();
                    return null;
                }
            };
        }
    }

    public BgTask(String label, Callable<V> callable) {
        this(label, callable, ProgressObserver.NULL_OBSERVER, EmptyBgTaskListener.get());
    }

    public BgTask(String label, Callable<V> callable, ProgressObserver observer, BgTaskListener<? super V> listener) {
        this.requestProcessor = TmcRequestProcessor.instance;
        this.label = label;
        this.listener = listener;
        this.callable = callable;
        this.proressObserver = observer;
        this.progressHandle = null;
    }

    public Future<V> start() {
        return requestProcessor.submit(this);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public V call() {
        synchronized (cancelLock) {
            if (cancelled) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.bgTaskCancelled();
                    }
                });
                return null;
            } else {
                executingThread = Thread.currentThread();
            }
        }

        if (progressHandle == null) {
            progressHandle = ProgressHandleFactory.createSystemHandle(label, this);
        }

        if (proressObserver instanceof BridgingProgressObserver) {
            BridgingProgressObserver bi =(BridgingProgressObserver) this.proressObserver;
            bi.attach(progressHandle);
        }

        progressHandle.start();
        try {
            final V result = callable.call();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.bgTaskReady(result);
                }
            });
            return result;
        } catch (InterruptedException e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.bgTaskCancelled();
                }
            });
            return null;
        } catch (final Exception ex) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.bgTaskFailed(ex);
                }
            });
            return null;
        } catch (final Throwable ex) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.bgTaskFailed(ex);
                }
            });
            throw new RuntimeException(ex);
        } finally {
            synchronized (cancelLock) {
                executingThread = null;
            }
            progressHandle.finish();
        }
    }

    @Override
    public boolean cancel() {
        synchronized (cancelLock) {
            cancelled = true;
            if (callable instanceof Cancellable) {
                return ((Cancellable)callable).cancel();
            } else {
                if (executingThread != null) {
                    executingThread.interrupt();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
