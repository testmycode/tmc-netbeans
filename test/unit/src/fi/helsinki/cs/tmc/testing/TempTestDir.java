package fi.helsinki.cs.tmc.testing;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;

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
        FileUtils.forceDelete(dir);
    }
}
