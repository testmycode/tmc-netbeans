
package fi.helsinki.cs.tmc.utilities;

import com.google.common.util.concurrent.FutureCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * When multiple listeners/callback are needed for one action, they should be added to 
 * FutureCallbackList.
 * Then the first callback can execute the rest by onSuccess method.
 * 
 * @param <T> can be any Object defined by the return value of the ListenableFuture.
 */
public class FutureCallbackList<T> {

    private List<FutureCallback<T>> callbacks;

    public FutureCallbackList() {
        this.callbacks = new ArrayList<FutureCallback<T>>();
    }

    public void addListener(FutureCallback<T> callback) {
        this.callbacks.add(callback);
    }
    
    /**
     * Calls the onSuccess of all callbacks.
     * @param result that should be shared to all callbacks.
     */
    public void onSuccess(T result) {
        for (FutureCallback<T> callback : callbacks) {
            callback.onSuccess(result);
        }
    }
    
    /**
     * Calls the onFailure of all callbacks.
     */
    public void onFailure(Throwable ex) {
        for (FutureCallback<T> callback : callbacks) {
            callback.onFailure(ex);
        }
    }
}
