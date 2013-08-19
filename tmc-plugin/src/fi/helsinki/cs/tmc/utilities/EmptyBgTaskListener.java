package fi.helsinki.cs.tmc.utilities;

public class EmptyBgTaskListener {
    public static <T> BgTaskListener<T> get(Class<T> cls) {
        return new BgTaskListener<T>() {
            @Override
            public void bgTaskReady(T result) {
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
            }
        };
    }
}
