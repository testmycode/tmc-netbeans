/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.json.updaters;

import fi.helsinki.cs.tmc.utilities.http.FileDownloaderAsync;
import fi.helsinki.cs.tmc.utilities.http.IDownloadListener;
import java.io.InputStream;
import fi.helsinki.cs.tmc.settings.LegacyPluginSettings;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.textio.StreamToString;
import fi.helsinki.cs.tmc.utilities.textio.WriteToFile;

/**
 * This class is used to download a list of courses in JSON format.
 * @author knordman
 */
@Deprecated
public class JSONCourseListUpdater implements IDownloadListener {

    private ICourseListUpdateListener listener;
    private FileDownloaderAsync downloader;

    private JSONCourseListUpdater() {
    }

    public JSONCourseListUpdater(String downloadAddress, ICourseListUpdateListener listener) throws Exception {
        this.listener = listener;
        downloader = new FileDownloaderAsync(downloadAddress, this);
        downloader.setTimeout(LegacyPluginSettings.getSettings().getCourseListDownloadTimeout());
    }

    /**
     * Method downloads course list
     */
    public void downloadCourseList() {
        try {
            downloader.download("Downloading course list");
        } catch (Exception ex) {
            listener.courseListDownloadFailed(ex.getMessage());
        }
    }

    /**
     * This method "cancels" course list download. (It closes course list download
     * progress bar and palikka doesn't anymore listen to course list download task.) 
     */
    public void cancel() {
        downloader.cancel();
    }

    /**
     * Method saves course list to CourseList.json file
     * @param in InputStream parameter
     * @throws Exception 
     */
    private void saveCourseList(InputStream in) throws Exception {
        String json = StreamToString.inputStreamToString(in);

        JSONCourseListParser.parseJson(json);

        WriteToFile writer = new WriteToFile();
        writer.writeToFile(json, "CourseList.json");
    }

    /**
     * Method informs if download has completed or failed
     * @param source 
     */
    @Override
    public void downloadCompleted(FileDownloaderAsync source) {
        try {
            saveCourseList(source.getFileContent());
        } catch (Exception ioex) {
            listener.courseListDownloadFailed(ioex.getMessage());
        }

        listener.courseListDownloadComplete();
    }

    /**
     * Method informs that download has failed
     * @param source 
     */
    @Override
    public void downloadFailed(FileDownloaderAsync source) {
        listener.courseListDownloadFailed(source.getErrorMsg());
    }
    
    /**
     * Because user has cancelled download informs method that download has failed
     * @param source 
     */
    @Override
    public void downloadCancelledByUser(FileDownloaderAsync source) {
        listener.courseListDownloadFailed(source.getErrorMsg());
    }
}
