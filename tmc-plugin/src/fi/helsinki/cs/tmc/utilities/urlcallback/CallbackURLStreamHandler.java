package fi.helsinki.cs.tmc.utilities.urlcallback;

import com.google.common.collect.Maps;
import org.openide.util.URLStreamHandlerRegistration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.util.Map;

/**
 * Handles URLs by calling callbacks.
 *
 * <p>
 * Handles URLs like {@code callback:///foo} where {@code foo} is a callback
 * registered via {@link #registerCallback(java.lang.String, fi.helsinki.cs.tmc.utilities.urlcallback.URLCallback)}.
 */
@URLStreamHandlerRegistration(protocol = "callback")
public class CallbackURLStreamHandler extends URLStreamHandler {

    private static final Map<String, URLCallback> callbacks = Maps.newHashMap();

    public static void registerCallback(Path path, URLCallback callback) {
        if (callback == null) {
            callbacks.remove(path.toString());
        } else {
            callbacks.put(path.toString(), callback);
        }
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new URLConnection(u) {
            private URLCallback callback;
            private InputStream is;

            @Override
            public void connect() throws FileNotFoundException {
                if (callback == null) {
                    callback = callbacks.get(url.getPath());
                }
                if (callback == null) {
                    throw new FileNotFoundException("No such callback: " + url.getPath());
                }
            }

            @Override
            public String getContentEncoding() {
                try {
                    connect();
                } catch (FileNotFoundException ex) {
                    return null;
                }
                return callback.getInputEncoding();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();

                if (is == null) {
                    is = callback.openInputStream();
                }

                if (is != null) {
                    return is;
                } else {
                    return super.getInputStream();
                }
            }
        };
    }
}
