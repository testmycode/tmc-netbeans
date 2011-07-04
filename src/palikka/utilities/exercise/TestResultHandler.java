package palikka.utilities.exercise;

import java.util.ArrayList;
import palikka.data.Exercise;
import palikka.utilities.IconAnnotator;
import palikka.utilities.http.FileDownloaderAsync;
import palikka.utilities.http.IDownloadListener;
import palikka.utilities.json.parsers.JSONTestResultsParser;
import palikka.utilities.textio.StreamToString;

/**
 * This class is in charge of handling the testresults that are returned from
 * the server for a single exercise.
 * @author knordman
 */
public class TestResultHandler implements IDownloadListener {

    private String downloadAddress;
    /**
     * Used to download the testresults
     */
    private FileDownloaderAsync downloader;
    /**
     * This listener wants to hear the testresults.
     */
    private ITestResultListener listener;
    private Exercise exercise;
    private boolean resultsDownloaded;
    /**
     * A list of testresults that didn't go well.
     */
    private ArrayList<String> failures;

    /**
     * Constructor
     * @param jsonLink Link to the testresult JSON file
     * @param listener Listens to what this TestResultHandler has to say.
     * @throws Exception If there is a problem with the downloader
     */
    public TestResultHandler(String jsonLink, ITestResultListener listener) throws Exception {
        this.downloadAddress = jsonLink;
        this.downloader = new FileDownloaderAsync(downloadAddress, this);
        this.downloader.setTimeout(30000);
        this.listener = listener;
        this.exercise = null;
        this.failures = new ArrayList<String>();
    }

    /**
     * Tell this handler to download the testresults.
     * @param exe 
     */
    public void execute(Exercise exe) {
        if (exe == null) {
            throw new NullPointerException("Exercise in TestResultHandler.execute() was null!");
        }

        this.exercise = exe;
        this.downloader.download("Downloading testresults");
    }

    /**
     * 
     * @return How many tests failed.
     */
    public int getNumberOfFailures() {
        if (!resultsDownloaded) {
            throw new IllegalStateException("Results aren't downloaded yet.");
        }

        return failures.size();
    }

    /**
     * 
     * @return List of failed tests
     */
    public ArrayList<String> getFailures() {
        if (!resultsDownloaded) {
            throw new IllegalStateException("Results aren't downloaded yet.");
        }

        return failures;
    }

    /**
     * 
     * @return The exercise that this handler is checking
     */
    public Exercise getExercise() {
        if (!resultsDownloaded) {
            throw new IllegalStateException("Results aren't downloaded yet.");
        }

        return exercise;
    }

    /**
     * Dislpay a dialog with failed tests and reasons if tests were failed.
     * If all tests passed, do nothing.
     * @param source 
     */
    @Override
    public void downloadCompleted(FileDownloaderAsync source) {
        resultsDownloaded = true;

        try {

            String json = StreamToString.inputStreamToString(source.getFileContent());
            failures = JSONTestResultsParser.parseJson(json);

            ExerciseStatus status = ExerciseStatus.getStatus(exercise);

            if (status.getStatus() != ExerciseStatus.Status.AllTestsPassed) {
                if (getNumberOfFailures() != 0) {
                    status.setStatus(ExerciseStatus.Status.SendAndSomeTestsFailed);

                } else {
                    status.setStatus(ExerciseStatus.Status.AllTestsPassed);
                }

                ExerciseStatus.writeToFile(status, exercise);
            }

            IconAnnotator.UpdateIcons();

            listener.runComplete(this);

        } catch (Exception ex) {
            listener.runFailed(ex.getMessage());
        }
    }

    /**
     * Called when the download fails.
     * @param source 
     */
    @Override
    public void downloadFailed(FileDownloaderAsync source) {
        listener.runFailed(source.getErrorMsg());
    }

    /**
     * Called when the download is cancelled.
     * @param source 
     */
    @Override
    public void downloadCancelledByUser(FileDownloaderAsync source) {
        listener.runFailed(source.getErrorMsg());
    }
}
