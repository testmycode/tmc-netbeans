package fi.helsinki.cs.tmc.utilities.urlcallback;

import java.io.InputStream;

public interface URLCallback {
    String getInputEncoding();
    InputStream openInputStream();
}
