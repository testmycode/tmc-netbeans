package fi.helsinki.cs.tmc.utilities;

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
    
    private static RequestProcessor defaultRequestProcessor =
            new RequestProcessor("BgTask processor", 5, true);
    
    private String label;
    private BgTaskListener<? super V> listener;
    private Callable<V> callable;
    private ProgressHandle progressHandle;
    
    private final Object cancelLock = new Object();
    private boolean cancelled;
    private Thread executingThread;
    
    public static <V> Future<V> start(String label, Callable<V> callable) {
        return new BgTask<V>(label, callable).start();
    }

    public static <V> Future<V> start(String label, Callable<V> callable, BgTaskListener<? super V> listener) {
        return new BgTask<V>(label, callable, listener).start();
    }

    public static Future<Object> start(String label, Runnable runnable) {
        Callable<Object> callable = runnableToCallable(runnable);
        return start(label, callable);
    }

    public static Future<Object> start(String label, Runnable runnable, BgTaskListener<Object> listener) {
        Callable<Object> callable = runnableToCallable(runnable);
        return start(label, callable, listener);
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
        this(label, callable, EmptyBgTaskListener.get());
    }
    
    public BgTask(String label, Callable<V> callable, BgTaskListener<? super V> listener) {
        this.label = label;
        this.listener = listener;
        this.callable = callable;
        this.progressHandle = null;
    }
    
    public Future<V> start() {
        return defaultRequestProcessor.submit(this);
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
        } catch (final Throwable t) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.bgTaskFailed(t);
                }
            });
            return null;
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
