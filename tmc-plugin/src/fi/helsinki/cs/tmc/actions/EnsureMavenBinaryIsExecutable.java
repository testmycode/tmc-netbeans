package fi.helsinki.cs.tmc.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class EnsureMavenBinaryIsExecutable {
    
    private final static Path RELATIVE_MAVEN_LOCATION = Paths.get("java").resolve("maven").resolve("bin").resolve("mvn");
    
    private final Path mavenPath;
    private final boolean isUnix;
    
    public EnsureMavenBinaryIsExecutable() {
        Path pathCandidate = Paths.get(System.getProperty("user.dir")).resolve(RELATIVE_MAVEN_LOCATION);
        if (Files.exists(pathCandidate)) {
            this.mavenPath = pathCandidate;
        } else {
            this.mavenPath = Paths.get(System.getProperty("user.dir")).resolve("../").resolve(RELATIVE_MAVEN_LOCATION);
        }
        this.isUnix = !System.getProperty("os.name").startsWith("Windows");
    }
    
    public void run() {
        if (!isUnix || !Files.exists(mavenPath)) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(mavenPath);
            if (!permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(mavenPath, permissions);
            }
        } catch (IOException ex) { }
    }
}
