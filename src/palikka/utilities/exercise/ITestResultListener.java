package palikka.utilities.exercise;

/**
 * Allows Controller to listen to testresults
 * @author knordman
 */
public interface ITestResultListener {
    void runComplete(TestResultHandler handler);
    void runFailed(String errorMsg);
}
