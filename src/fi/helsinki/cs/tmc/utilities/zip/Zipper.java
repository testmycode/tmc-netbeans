/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author knordman
 */
public class Zipper {

    private static Zipper instance;
    
    public static Zipper getDefault() {
        if (instance == null) {
            instance = new Zipper();
        }
        return instance;
    }
    
    /**
     * Constructor
     */
    public Zipper() {
    }

    /**
     * Used to write a single entry into the given ZipOutputStream.
     * @param file File to read.
     * @param out Stream to write to.
     * @param path Folder in which the file resides in.
     * @throws IOException 
     */
    private void writeEntry(File file, ZipOutputStream out, String path) throws IOException {
        byte[] buffer = new byte[1024];
        out.putNextEntry(new ZipEntry(path + File.separator + file.getName()));

        FileInputStream in = new FileInputStream(file);

        int len;
        while ((len = in.read(buffer)) > 0) {  //write the entry (file) to the zipfile
            out.write(buffer, 0, len);
        }

        in.close();
        out.closeEntry();
    }

    /**
     * Used to create a zip file recursively.
     * @param file Directory to be zipped.
     * @param out Stream to write to.
     * @param path Directory path, can be an empty string.
     * @throws IOException 
     * @throws NullPointerException
     */
    private void zipFileRecur(File file, ZipOutputStream out, String path) throws IOException, NullPointerException {
        String currentPath;
        if (!path.equals("")) {
            currentPath = path + File.separator + file.getName();
        } else {
            currentPath = file.getName();
        }

        out.putNextEntry(new ZipEntry(currentPath + File.separator));  //Add the folder to the zip file
        out.closeEntry();

        File[] files = file.listFiles();
        for (File singleFile : files) {
            if (singleFile.isDirectory()) {
                zipFileRecur(singleFile, out, currentPath);
            } else {
                writeEntry(singleFile, out, currentPath);
            }
        }
    }

    /**
     * Creates a zip file.
     * @param path Must point to a project folder.
     * @return byte[] Zip file data in byte array.
     * @throws IOException 
     * @throws NullPointerException
     * @throws FileNotFoundException
     */
    public byte[] zip(String path) throws IOException, NullPointerException, FileNotFoundException {
        if (path == null) {
            throw new NullPointerException("path cannot be null");
        }

        return zip(new File(path));
    }

    /**
     * Create a zipfile containing the project folder and only the "src" folder inside.
     * @param file Should be a project folder.
     * @return byte[] Containing the zipped file.
     * @throws IOException If file doesn't exist or is unreadable.
     * @throws FileNotFoundException
     * @throws NullPointerException
     */
    public byte[] zip(File file) throws IOException, FileNotFoundException, NullPointerException {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }

        if (!file.exists()) {
            throw new FileNotFoundException("File not found!");
        }

        ByteArrayOutputStream zipContent = new ByteArrayOutputStream();

        ZipOutputStream out = new ZipOutputStream(zipContent);

        zipFileRecur(file, out, "");
        out.close();

        return zipContent.toByteArray();
    }
}
