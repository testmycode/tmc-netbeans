/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities.json.updaters;

import palikka.utilities.http.FileDownloaderAsync;
import palikka.utilities.http.IDownloadListener;
import java.io.InputStream;
import palikka.settings.PluginSettings;
import palikka.utilities.json.parsers.JSONCourseListParser;
import palikka.utilities.textio.StreamToString;
import palikka.utilities.textio.WriteToFile;

/**
 * This class is used to download a list of courses in JSON format.
 * @author knordman
 */
public class JSONCourseListUpdater implements IDownloadListener {

    private ICourseListUpdateListener listener;
    private FileDownloaderAsync downloader;

    private JSONCourseListUpdater() {
    }

    public JSONCourseListUpdater(String downloadAddress, ICourseListUpdateListener listener) throws Exception {
        this.listener = listener;
        downloader = new FileDownloaderAsync(downloadAddress, this);
        downloader.setTimeout(PluginSettings.getSettings().getCourseListDownloadTimeout());
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
