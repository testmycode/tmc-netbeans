package palikka.utilities.http;

/**
 * Used to listen for results from uploads performed by FileUploaderAsync.
 * @author jmturpei
 */
public interface IUploadListener {

    public void uploadCompleted(FileUploaderAsync source);

    public void uploadFailed(FileUploaderAsync source);
}
