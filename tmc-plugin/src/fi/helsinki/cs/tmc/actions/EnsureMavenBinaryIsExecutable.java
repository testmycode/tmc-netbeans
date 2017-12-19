package fi.helsinki.cs.tmc.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.openide.modules.InstalledFileLocator;

public class EnsureMavenBinaryIsExecutable {
    
    private final static Path RELATIVE_MAVEN_LOCATION = Paths.get("java").resolve("maven").resolve("bin").resolve("mvn");
    
    private Path mavenPath;
    private final boolean isUnix;
    
    public EnsureMavenBinaryIsExecutable() {
        this.isUnix = !System.getProperty("os.name").startsWith("Windows");
    }
    
    public void run() {
        if (!isUnix) {
            return;
        }
        Path pathCandidate = Paths.get(System.getProperty("user.dir")).resolve(RELATIVE_MAVEN_LOCATION);
        if (Files.exists(pathCandidate)) {
            this.mavenPath = pathCandidate;
        } else {
            this.mavenPath = Paths.get(System.getProperty("user.dir")).resolve("../").resolve(RELATIVE_MAVEN_LOCATION);
        }
        for (File file : InstalledFileLocator.getDefault().locateAll(".", null, false)) {
            tryToChmod(Paths.get(file.getAbsolutePath()).resolve("..").resolve(RELATIVE_MAVEN_LOCATION));
        }
        if (!Files.exists(mavenPath)) {
            return;
        }
        tryToChmod(mavenPath);
    }

    private void tryToChmod(Path path) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            if (!permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(path, permissions);
            }
        } catch (IOException ex) { }
    }
}
