package fi.helsinki.cs.tmc.utilities.zip;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    
    public void unzipProject(byte[] data, File projectRoot, String projectName) throws IOException {
        String projectDirInZip = findProjectDirInZip(data);
        if (projectDirInZip == null) {
            throw new IllegalArgumentException("No project directory in zip");
        }
        
        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.getName().startsWith(projectDirInZip)) {
                String restOfPath = zent.getName().substring(projectDirInZip.length());
                String newPath = projectName + "/" + restOfPath;
                File destFile = new File(
                        projectRoot.getAbsolutePath() + File.separator +
                        newPath.replaceAll("/", File.separator)
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
    
    @Deprecated
    public void unZip(byte[] data, String unzipPath, String projectName) throws IOException {
        unZip(new ByteArrayInputStream(data), unzipPath);
    }

    /**
     * Unzips a file to the same folder as the original zipped file.
     * @param in Stream to read.
     * @param unzipPath Should be a full path without "/" at the end.
     * @return Name of the first unzipped file, should be a directory. [TODO: remove]
     * @throws IOException
     */
    @Deprecated
    public String unZip(InputStream in, String unzipPath) throws IOException {
        unzipPath += File.separator;
        (new File(unzipPath)).mkdirs();
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry entry = null;
        byte[] buffer = new byte[1024];

        String firstFile = null;  //The first file that is being unzipped should be the projects parent folder.
        boolean first = true;

        while ((entry = zin.getNextEntry()) != null) {

            if (first) {
                firstFile = entry.getName();
                first = false;
            }

            (new File(unzipPath + entry.getName())).mkdirs();

            if (entry.isDirectory()) {
                continue;
            }

            File currentFile = new File(unzipPath + entry.getName());
            if (currentFile.exists()) {
                if (!currentFile.delete()) {
                    throw new IOException("Unable to overwrite an old file!");
                }
            }

            FileOutputStream out = new FileOutputStream(currentFile);
            int len;

            while ((len = zin.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }

            zin.closeEntry();
            out.close();
        }
        zin.close();
        return firstFile;

    }
}
