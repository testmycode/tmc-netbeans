/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities;

import java.io.File;
import java.io.FileFilter;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.settings.PluginSettings;

/**
 *
 * @author jmturpei
 */
public class FolderHelper {

    /**
     * Search subfolders by folder name.
     * @param folder Folder where to begin search.
     * @param folderName Folder to search.
     * @return File Matching folder or null, if no folder found.
     */
    public static File searchInnerFolder(File folder, String folderName) {
        if (folder == null) {
            return null;
        }
        if (!folder.exists()) {
            return null;
        }
        if (folderName == null || folderName.isEmpty()) {
            return null;
        }

        if (folder.getName().equals(folderName)) {
            return folder;
        }


        File[] Folders = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File childFolder : Folders) {
            if (childFolder.getName().equals(folderName)) {
                return childFolder;
            }
        }

        for (File childFolder : Folders) {
            File result = searchInnerFolder(childFolder, folderName);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    
    
    /**
     * Search "src" folder.
     * @param exercise Related exercise for subfolder search.
     * @return File Exercise "src" folder.
     */
    public static File searchSrcFolder(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise was null at FolderHelper.searchSrcFolder");
        }
        return searchInnerFolder(generatePath(exercise), "src");
    }

    
    
    /**
     * Search project folder.
     * @param exercise Related exercise for subfolder search.
     * @return File Exercise project folder.
     */
    public static File searchNbProject(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise was null at FolderHelper.searchNbProject");
        }
        return searchNbProject(generatePath(exercise));
    }

    
    
    /**
     * Search project folder.
     * @param root Root folder where to begin search, can be null.
     * @return File Netbeans project folder if found, otherwise null.
     */
    public static File searchNbProject(File root) {
        File folder = searchInnerFolder(root, "nbproject");

        if (folder == null) {
            return null;
        }

        String parent = folder.getParent();
        if (parent == null) {
            return null;
        }

        return new File(parent);
    }

    
    
    /**
     * Method generates a unique path to an exercise:
     * /default folder/course name/exercise name/ 
     * @param exercise Exercise for which to generate path.
     * @return File Path to the exercise.
     * @throws NullPointerException 
     */
    public static File generatePath(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise was null at FolderHelper.generatePath");
        }

        String path = PluginSettings.getSettings().getDefaultFolder();
        if (path == null) {
            return null;
        }

        path += PalikkaConstants.fileSeparator + exercise.getCourse().getName();
        path += PalikkaConstants.fileSeparator + exercise.getName();

        return new File(path);
    }
}
