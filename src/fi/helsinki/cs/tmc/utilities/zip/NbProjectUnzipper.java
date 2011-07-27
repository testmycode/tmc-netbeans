package fi.helsinki.cs.tmc.utilities.zip;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
public class NbProjectUnzipper {

    private static NbProjectUnzipper instance;
    
    public static NbProjectUnzipper getDefault() {
        if (instance == null) {
            instance = new NbProjectUnzipper();
        }
        return instance;
    }

    public NbProjectUnzipper() {
    }

    
    public void unzipProject(byte[] data, File projectsRootDir, String projectName) throws IOException {
        String projectDirPath = projectsRootDir.getAbsolutePath() + File.separator + projectName;
        if (new File(projectDirPath).exists()) {
            throw new IllegalStateException("The project directory '" + projectName + "' already exists.");
        }
        
        String projectDirInZip = findProjectDirInZip(data);
        if (projectDirInZip == null) {
            throw new IllegalArgumentException("No project directory in zip");
        }
        
        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.getName().startsWith(projectDirInZip)) {
                String restOfPath = zent.getName().substring(projectDirInZip.length());
                File destFile = new File(
                        projectDirPath + File.separator +
                        restOfPath.replace("/", File.separator)
                        );
                
                if (zent.isDirectory()) {
                    FileUtils.forceMkdir(destFile);
                } else {
                    FileUtils.forceMkdir(destFile.getParentFile());
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                    IOUtils.copy(zis, out);
                    out.close();
                }
            }
        }
    }
    
    private String findProjectDirInZip(byte[] data) throws IOException {
        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.getName().endsWith("/nbproject/")) {
                return dirname(zent.getName());
            }
        }
        return null;
    }
    
    private String dirname(String zipPath) {
        while (zipPath.endsWith("/")) {
            zipPath = zipPath.substring(0, zipPath.length() - 1);
        }
        return zipPath.replaceAll("/[^/]+$", "");
    }
    
    private ZipInputStream readZip(byte[] data) {
        return new ZipInputStream(new ByteArrayInputStream(data));
    }
}
