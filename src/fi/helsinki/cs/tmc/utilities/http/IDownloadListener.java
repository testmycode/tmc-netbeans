package fi.helsinki.cs.tmc.utilities.http;

/**
 * Used to listen for results from downloads performed by FileDownloaderAsync.
 * @author jmturpei
 */
public interface IDownloadListener {

    public void downloadCompleted(FileDownloaderAsync source);

    public void downloadFailed(FileDownloaderAsync source);

    public void downloadCancelledByUser(FileDownloaderAsync source);
}
