/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.zip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author knordman
 */
public class Unzipper {

    private static Unzipper instance;
    
    public static Unzipper getDefault() {
        if (instance == null) {
            instance = new Unzipper();
        }
        return instance;
    }

    public Unzipper() {
    }

    public void unZip(byte[] data, String unzipPath) throws IOException {
        unZip(new ByteArrayInputStream(data), unzipPath);
    }

    /**
     * Unzips a file to the same folder as the original zipped file.
     * @param in Stream to read.
     * @param unzipPath Should be a full path without "/" at the end.
     * @return Name of the first unzipped file, should be a directory. [TODO: remove]
     * @throws IOException
     */
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
