/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.json.updaters;

import fi.helsinki.cs.tmc.utilities.http.FileDownloaderAsync;
import fi.helsinki.cs.tmc.utilities.http.IDownloadListener;
import java.io.InputStream;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.settings.PluginSettings;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import fi.helsinki.cs.tmc.utilities.textio.StreamToString;
import fi.helsinki.cs.tmc.utilities.textio.WriteToFile;

/**
 * This class is used to download a list of exercises in JSON format.
 * @author knordman
 */
public class JSONExerciseListUpdater implements IDownloadListener {

    private String filename;
    private IExerciseListUpdateListener listener;
    private FileDownloaderAsync downloader;
    private Course course;

    private JSONExerciseListUpdater() {
    }

    public JSONExerciseListUpdater(String downloadAddress, IExerciseListUpdateListener listener) throws Exception {

        this.listener = listener;
        downloader = new FileDownloaderAsync(downloadAddress, this);
        downloader.setTimeout(PluginSettings.getSettings().getExerciseListDownloadTimeout());
    }

    /**
     * Downloads a given course and writes it to a json file
     * @param course 
     */
    public void downloadAndWriteToFile(Course course) {
        this.filename = course.getName() + ".json";
        this.course = course;

        downloader.download("Downloading exercise list");
    }

    /**
     * Method saves exercise list to a file
     * @param in
     * @throws Exception 
     */
    private void saveExerciseList(InputStream in) throws Exception {

        String json = StreamToString.inputStreamToString(in);

        JSONExerciseListParser.parseJson(json, course); //parseJson method is used here to check 
        //that created json parameter is usable
        WriteToFile writer = new WriteToFile();
        writer.writeToFile(json, filename);
    }

    /**
     * Method informs if download has succeeded or not
     * @param source 
     */
    @Override
    public void downloadCompleted(FileDownloaderAsync source) {
        try {
            saveExerciseList(source.getFileContent());
        } catch (Exception ioex) {
            listener.exerciseListUpdateFailed(ioex.getMessage());
            return;
        }

        listener.exerciseListUpdateComplete();
    }

    /**
     * Method informs that download has failed
     * @param source 
     */
    @Override
    public void downloadFailed(FileDownloaderAsync source) {
        listener.exerciseListUpdateFailed(source.getErrorMsg());
    }

    /**
     * Because user has cancelled download method informs that download has failed
     * @param source 
     */
    @Override
    public void downloadCancelledByUser(FileDownloaderAsync source) {
        listener.exerciseListUpdateFailed(source.getErrorMsg());
    }
}
