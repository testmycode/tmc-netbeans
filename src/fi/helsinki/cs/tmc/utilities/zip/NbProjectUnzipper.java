package fi.helsinki.cs.tmc.utilities.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class NbProjectUnzipper {

    public static interface OverwritingDecider {
        /**
         * Decides whether the given relative path in the project may be overwritten.
         * 
         * <p>
         * Only called for files (not directories) whose content has changed.
         * 
         * <p>
         * Note that the given path has platform-specific directory separators.
         */
        public boolean canOverwrite(String relPath);
    }
    
    /**
     * Information about the results of an unzip operation.
     * 
     * <p>
     * All lists contain paths relative to the project directory.
     * Directories are not included.
     */
    public class Result {
        /**
         * The project directory to which we extracted.
         */
        public File projectDir;
        
        /**
         * Files that were in the zip but did not exist before.
         * In the usual case of downloading a new project, all files go here.
         */
        public List<String> newFiles = new ArrayList<String>();
        
        /**
         * Files overwritten as permitted by the given {@code OverwritingDecider}.
         */
        public List<String> overwrittenFiles = new ArrayList<String>();
        
        /**
         * Files skipped because the given {@code OverwritingDecider} didn't allow overwriting.
         */
        public List<String> skippedFiles = new ArrayList<String>();
        
        /**
         * Files that existed before but were the same in the zip.
         */
        public List<String> unchangedFiles = new ArrayList<String>();
        
        Result(File projectDir) {
            this.projectDir = projectDir;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("New files: ").append(newFiles).append('\n');
            sb.append("Overwritten files: ").append(overwrittenFiles).append('\n');
            sb.append("Skipped files: ").append(skippedFiles).append('\n');
            sb.append("Unchanged files: ").append(unchangedFiles).append('\n');
            return sb.toString();
        }
    }
    
    private static OverwritingDecider neverAllowOverwrites = new OverwritingDecider() {
        @Override
        public boolean canOverwrite(String relPath) {
            return false;
        }
    };
    
    private static NbProjectUnzipper instance;
    
    public static NbProjectUnzipper getDefault() {
        if (instance == null) {
            instance = new NbProjectUnzipper();
        }
        return instance;
    }

    public NbProjectUnzipper() {
    }

    
    public Result unzipProject(byte[] data, File projectDir, String projectName) throws IOException {
        return unzipProject(data, projectDir, projectName, neverAllowOverwrites);
    }
    
    public Result unzipProject(byte[] data, File projectDir, String projectName, OverwritingDecider overwriting) throws IOException {
        return unzipProject(data, projectDir, projectName, overwriting, true);
    }
    
    public Result unzipProject(byte[] data, File projectDir, String projectName, OverwritingDecider overwriting, boolean reallyWriteFiles) throws IOException {
        Result result = new Result(projectDir);
        
        String projectDirInZip = findProjectDirInZip(data);
        if (projectDirInZip == null) {
            throw new IllegalArgumentException("No project directory in zip");
        }
        
        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.getName().startsWith(projectDirInZip)) {
                String restOfPath = zent.getName().substring(projectDirInZip.length());
                if (restOfPath.startsWith("/")) {
                    restOfPath = restOfPath.substring(1);
                }
                
                String destFileRelativePath = restOfPath.replace("/", File.separator);
                File destFile = new File(
                        projectDir.toString() + File.separator + destFileRelativePath
                        );
                
                if (zent.isDirectory()) {
                    if (reallyWriteFiles) {
                        FileUtils.forceMkdir(destFile);
                    }
                } else {
                    byte[] entryData = IOUtils.toByteArray(zis);

                    boolean shouldWrite;
                    if (destFile.exists()) {
                        if (fileContentEquals(destFile, entryData)) {
                            shouldWrite = true;
                            result.unchangedFiles.add(destFileRelativePath);
                        } else if (overwriting.canOverwrite(destFileRelativePath)) {
                            shouldWrite = true;
                            result.overwrittenFiles.add(destFileRelativePath);
                        } else {
                            shouldWrite = false;
                            result.skippedFiles.add(destFileRelativePath);
                        }
                    } else {
                        shouldWrite = true;
                        result.newFiles.add(destFileRelativePath);
                    }
                    if (shouldWrite && reallyWriteFiles) {
                        FileUtils.forceMkdir(destFile.getParentFile());
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                        IOUtils.write(entryData, out);
                        out.close();
                    }
                }
            }
        }
        
        return result;
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
    
    private boolean fileContentEquals(File file, byte[] data) throws IOException {
        InputStream fileIs = new BufferedInputStream(new FileInputStream(file));
        InputStream dataIs = new ByteArrayInputStream(data);
        return IOUtils.contentEquals(fileIs, dataIs);
    }
}
