package fi.helsinki.cs.tmc.utilities.zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Deprecated
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
        public boolean mayOverwrite(String relPath);

        /**
         * Decides whether the given relative path in the project may be deleted.
         *
         * <p>
         * Only called for files and directories that are on disk but not in the zip.
         *
         * <p>
         * Note that the given path has platform-specific directory separators.
         */
        public boolean mayDelete(String relPath);
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

        /**
         * Files that were deleted because they weren't in the zip.
         */
        public List<String> deletedFiles = new ArrayList<String>();

        /**
         * Files skipped because the given {@code OverwritingDecider} didn't allow deleting.
         */
        public List<String> skippedDeletingFiles = new ArrayList<String>();

        Result(File projectDir) {
            this.projectDir = projectDir;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("New: ").append(newFiles).append('\n');
            sb.append("Overwritten: ").append(overwrittenFiles).append('\n');
            sb.append("Skipped: ").append(skippedFiles).append('\n');
            sb.append("Unchanged: ").append(unchangedFiles).append('\n');
            sb.append("Deleted: ").append(deletedFiles).append('\n');
            sb.append("Not deleted: ").append(deletedFiles).append('\n');
            return sb.toString();
        }
    }

    private static OverwritingDecider neverAllowOverwrites = new OverwritingDecider() {
        @Override
        public boolean mayOverwrite(String relPath) {
            return false;
        }

        @Override
        public boolean mayDelete(String relPath) {
            return false;
        }
    };

    private OverwritingDecider overwriting;


    public NbProjectUnzipper() {
        this(neverAllowOverwrites);
    }

    public NbProjectUnzipper(OverwritingDecider overwriting) {
        this.overwriting = overwriting;
    }

    public Result unzipProject(byte[] data, File projectDir) throws IOException {
        return unzipProject(data, projectDir, true);
    }

    public Result unzipProject(byte[] data, File projectDir, boolean reallyWriteFiles) throws IOException {
        Result result = new Result(projectDir);
        Set<String> pathsInZip = new HashSet<String>();

        String projectDirInZip = findProjectDirInZip(data);
        if (projectDirInZip == null) {
            throw new IllegalArgumentException("No project directory in zip");
        }

        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.getName().startsWith(projectDirInZip)) {
                String restOfPath = zent.getName().substring(projectDirInZip.length());
                restOfPath = trimSlashes(restOfPath);

                String destFileRelativePath = trimSlashes(restOfPath.replace("/", File.separator));
                pathsInZip.add(destFileRelativePath);
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
                            shouldWrite = false;
                            result.unchangedFiles.add(destFileRelativePath);
                        } else if (overwriting.mayOverwrite(destFileRelativePath)) {
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

        deleteFilesNotInZip(projectDir, projectDir, result, pathsInZip, overwriting, reallyWriteFiles);

        return result;
    }

    private void deleteFilesNotInZip(File projectDir, File curDir, Result result, Set<String> pathsInZip, OverwritingDecider overwriting, boolean reallyWriteFiles) throws IOException {
        for (File file : curDir.listFiles()) {
            String relPath = file.getPath().substring(projectDir.getPath().length());
            relPath = trimSlashes(relPath);

            if (file.isDirectory()) {
                deleteFilesNotInZip(projectDir, file, result, pathsInZip, overwriting, reallyWriteFiles);
            }

            if (!pathsInZip.contains(relPath)) {
                if (overwriting.mayDelete(relPath)) {
                    if (file.isDirectory() && file.listFiles().length > 0) {
                        // Won't delete directories if they still have contents
                        result.skippedDeletingFiles.add(relPath);
                    } else {
                        if (reallyWriteFiles) {
                            file.delete();
                        }
                        result.deletedFiles.add(relPath);
                    }
                } else {
                    result.skippedDeletingFiles.add(relPath);
                }
            }
        }
    }

    private String trimSlashes(String s) {
        while (s.startsWith("/") || s.startsWith(File.separator)) {
            s = s.substring(1);
        }
        while (s.endsWith("/") || s.startsWith(File.separator)) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String findProjectDirInZip(byte[] data) throws IOException {
        ZipInputStream zis = readZip(data);
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            String name = zent.getName();
            if (name.endsWith("/nbproject/") || name.endsWith("/pom.xml") || name.endsWith(".universal/")) {
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
        boolean eq = IOUtils.contentEquals(fileIs, dataIs);
        fileIs.close();
        dataIs.close();
        return eq;
    }
}