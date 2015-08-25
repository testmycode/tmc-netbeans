package fi.helsinki.cs.tmc.testing;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class TempTestDir {
    private static AtomicInteger counter = new AtomicInteger(1);
    private final File dir;

    public TempTestDir() throws IOException {
        dir = new File("tmp" + File.separator + "test" + counter.getAndIncrement());
        FileUtils.forceMkdir(dir);
        FileUtils.forceDelete(dir);
        FileUtils.forceMkdir(dir);
    }

    public File get() {
        return dir;
    }

    public String getPath() {
        return get().getAbsolutePath();
    }

    public void destroy() throws IOException {
        try {
            FileUtils.forceDelete(dir);
        } catch (IOException e) {
            throw new IOException("Failed to delete temporary dir: " +
                        e.getMessage() +
                        ". If you're on Windows, ensure your test closes all open file handles.");
        }
    }
}
