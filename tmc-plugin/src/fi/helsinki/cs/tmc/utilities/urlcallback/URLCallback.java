package fi.helsinki.cs.tmc.utilities.urlcallback;

import java.io.InputStream;

public interface URLCallback {
    public String getInputEncoding();

    public InputStream openInputStream();
}
