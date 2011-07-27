package fi.helsinki.cs.tmc.utilities.zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

public class NbProjectZipper {

    private static NbProjectZipper instance;
    
    public static NbProjectZipper getDefault() {
        if (instance == null) {
            instance = new NbProjectZipper();
        }
        return instance;
    }
    
    public NbProjectZipper() {
    }
    
    /**
     * Zip up a project directory, only including the "src" subdirectory.
     */
    public byte[] zipProjectSources(String path) throws IOException {
        return zipProjectSources(new File(path));
    }


    private byte[] zipProjectSources(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new FileNotFoundException("Project directory not found for zipping!");
        }

        String rootDirName = directory.getName();
        
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(zipBuffer);
        
        try {
            zos.putNextEntry(new ZipEntry(rootDirName + "/"));
            zipRecursively(new File(directory + File.separator + "src"), zos, rootDirName);
        } finally {
            zos.close();
        }

        return zipBuffer.toByteArray();
    }

    private void writeEntry(File file, ZipOutputStream zos, String zipPath) throws IOException {
        zos.putNextEntry(new ZipEntry(zipPath + "/" + file.getName()));

        FileInputStream in = new FileInputStream(file);
        IOUtils.copy(in, zos);
        in.close();
        zos.closeEntry();
    }

    /**
     * Zips a directory recursively.
     */
    private void zipRecursively(File dir, ZipOutputStream zos, String parentZipPath) throws IOException {
        String thisDirZipPath;
        if (parentZipPath.isEmpty()) {
            thisDirZipPath = dir.getName();
        } else {
            thisDirZipPath = parentZipPath + "/" + dir.getName();
        }

        // Create an entry for the directory
        zos.putNextEntry(new ZipEntry(thisDirZipPath + "/"));
        zos.closeEntry();

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                zipRecursively(file, zos, thisDirZipPath);
            } else {
                if (shouldIncludeFile(thisDirZipPath + "/" + file.getName())) {
                    writeEntry(file, zos, thisDirZipPath);
                }
            }
        }
    }

    private boolean shouldIncludeFile(String zipPath) {
        return zipPath.contains("/src/");
    }
}
