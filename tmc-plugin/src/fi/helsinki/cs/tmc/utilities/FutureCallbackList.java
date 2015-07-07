
package fi.helsinki.cs.tmc.utilities;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;


public class FutureCallbackList<T> {

    private List<FutureCallback<T>> callbacks;

    public FutureCallbackList() {
        this.callbacks = new ArrayList<FutureCallback<T>>();
    }

    public void addListener(FutureCallback<T> callback) {
        this.callbacks.add(callback);
    }
    
    public void onSuccess(T result) {
        for (FutureCallback<T> callback : callbacks) {
            callback.onSuccess(result);
        }
    }
    
    public void onFailure(Throwable ex) {
        for (FutureCallback<T> callback : callbacks) {
            callback.onFailure(ex);
        }
    }
}
