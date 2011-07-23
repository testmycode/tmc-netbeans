package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;

public class NetworkTasks {
    private static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;
    
    public CancellableCallable<byte[]> downloadFile(String url) {
        return new FileDownload(url);
    }
    
    public CancellableCallable<String> downloadTextFile(String url) {
        final FileDownload download = new FileDownload(url);
        return new CancellableCallable<String>() {
            @Override
            public String call() throws Exception {
                return new String(download.call(), "UTF-8");
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
}
