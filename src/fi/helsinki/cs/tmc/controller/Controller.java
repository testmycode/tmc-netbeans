package fi.helsinki.cs.tmc.controller;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.utilities.AdvancedDownloadFeature;
import fi.helsinki.cs.tmc.utilities.CourseAndExerciseInfo;
import fi.helsinki.cs.tmc.utilities.FolderHelper;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.ProjectHandler;
import fi.helsinki.cs.tmc.utilities.exercise.ExerciseDownloader;
import fi.helsinki.cs.tmc.utilities.exercise.ExerciseFilter;
import fi.helsinki.cs.tmc.utilities.exercise.ExerciseLoader;
import fi.helsinki.cs.tmc.utilities.exercise.ExerciseStatus;
import fi.helsinki.cs.tmc.utilities.exercise.ExerciseUploader;
import fi.helsinki.cs.tmc.utilities.exercise.IExerciseDownloadListener;
import fi.helsinki.cs.tmc.utilities.exercise.IExerciseUploadListener;
import fi.helsinki.cs.tmc.utilities.exercise.ITestResultListener;
import fi.helsinki.cs.tmc.utilities.exercise.TestResultHandler;
import fi.helsinki.cs.tmc.utilities.json.updaters.IExerciseListUpdateListener;
import fi.helsinki.cs.tmc.utilities.json.updaters.JSONExerciseListUpdater;
import fi.helsinki.cs.tmc.utilities.textio.StreamToString;
import fi.helsinki.cs.tmc.utilities.zip.Unzipper;
import java.io.File;
import java.io.InputStream;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ActionProvider;
import org.openide.LifecycleManager;

@Deprecated
public class Controller implements
        IController,
        IExerciseListUpdateListener,
        IExerciseDownloadListener,
        IExerciseUploadListener,
        ITestResultListener {

    /**
     * The singleton default controller.
     */
    private static Controller defaultController;
    
    /**
     * The active exercise downloader. Null if no download active.
     */
    private ExerciseDownloader exerciseDownloader;
    
    /**
     * The active exercise uploader. Null if no upload active.
     */
    private ExerciseUploader uploader;
    
    /**
     * The active exercise list downloader. Null if no download active.
     */
    private JSONExerciseListUpdater exerciseListDownloader;
    
    
    private JButton sendButton;
    private TestResultHandler testHandler;

    /**
     * Returns the default instance of the controller.
     */
    public static IController getInstance() {
        if (defaultController == null) {
            defaultController = new Controller();
        }

        return defaultController;
    }

    /**
     * Private singleton constructor.
     */
    private Controller() {
        uploader = null;
        sendButton = null;
    }

    /**
     * Downloads exercises that haven't expired and haven't already been downloaded.
     * It opens as projects newly and previously downloaded exercises that
     * aren't yet expired.
     */
    @Override
    public void startExerciseOpening() {
        Course course = CourseAndExerciseInfo.getCurrentCourse();

        if (course != null) {
            try {
                if (exerciseListDownloader != null) {
                    return;
                }
                exerciseListDownloader = new JSONExerciseListUpdater(course.getExerciseListDownloadAddress(), this);

            } catch (Exception e) {
                ModalDialogDisplayer.getDefault().displayError(e);
                return;
            }

            exerciseListDownloader.downloadAndWriteToFile(course);
        } else {
            ModalDialogDisplayer.getDefault().displayError("No course selected. Please select course from preferences");

        }
    }

    /**
     * Download all the exercises in the given ExerciseCollection regardless
     * of expiration dates and such.
     */
    @Override
    public void advancedDownload(ExerciseCollection collection) {
        if (exerciseDownloader != null) {
            ModalDialogDisplayer.getDefault().displayError("Files are still downloading. Try again later.");
            return;
        } else {
            exerciseDownloader = new ExerciseDownloader(collection, this);
            try {
                exerciseDownloader.downloadExercises();
            } catch (Exception ex) {
                ModalDialogDisplayer.getDefault().displayError(ex);
            }
        }
    }

    /**
     * Show the advanced download dialog
     */
    @Override
    public void showAdvancedDownload() {
        try {
            AdvancedDownloadFeature.show();
        } catch (Exception ex) {
            ModalDialogDisplayer.getDefault().displayError(ex);
        }
    }

    /* Exercise list download */
    @Override
    public void exerciseListUpdateComplete() {
        exerciseListDownloader = null;
        showAllExercises();
    }

    @Override
    public void exerciseListUpdateFailed(String errorMessage) {
        exerciseListDownloader = null;
        ModalDialogDisplayer.getDefault().displayError("exercise list update failed. Unable to download new exercises from server.\r\n Detailed error msg: " + errorMessage);
        showAllExercises();
    }

    /**
     * Opens all local exercises and downloads the rest. The rest open as they
     * are downloaded.
     */
    private void showAllExercises() {
        ExerciseCollection exercises = CourseAndExerciseInfo.getCurrentExerciseList();

        if (exercises == null) {
            ModalDialogDisplayer.getDefault().displayError("cannot find exercise list. Unable to open any exercise");
            return;
        }

        ProjectManager.getDefault().clearNonProjectCache();
        ExerciseCollection nonExpiredExercises = ExerciseFilter.getNonExpired(exercises, new Date());

        ExerciseCollection localExercises = ExerciseFilter.getLocal(nonExpiredExercises);
        ExerciseCollection downloadableExercises = ExerciseFilter.getDownloadable(nonExpiredExercises);

        try {
            ExerciseLoader.openAll(localExercises);
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError(e);
        }

        if (exerciseDownloader != null) {
            return; // one loader is already running. We don't want another.
        }

        exerciseDownloader = new ExerciseDownloader(downloadableExercises, this);
        try {
            exerciseDownloader.downloadExercises();
        } catch (Exception ex) {
            ModalDialogDisplayer.getDefault().displayError(ex);
        }
    }

    
    /* Exercise download */
    /**
     * Called by the ExerciseDownloader when it has finished downloading an
     * exercise. It unzips the exercise and opens it as a project.
     */
    @Override
    public void exerciseDownloadCompleted(Exercise downloadedExercise, InputStream fileContent) {
        File oldSources = FolderHelper.searchSrcFolder(downloadedExercise);

        if (oldSources != null) {
            String msg = "Old version of exercise " + downloadedExercise.getName() + " found on disk.\r\n";
            msg += "Source code location: " + oldSources.getAbsolutePath() + "\r\n";
            msg += "All modified source codes will be lost. Do you want to continue? ";
            int res = JOptionPane.showConfirmDialog(null, msg, "Warning", JOptionPane.OK_CANCEL_OPTION);

            if (res == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        
        Unzipper unzipper = new Unzipper();

        try {
            unzipper.unZip(fileContent, FolderHelper.generatePath(downloadedExercise).getAbsolutePath());
            ExerciseLoader.open(downloadedExercise);
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError(e);
        }

        ExerciseStatus.resetStatus(downloadedExercise);
    }

    @Override
    public void exerciseDownloadFailed(String errorMsg) {
        ModalDialogDisplayer.getDefault().displayError("download failed: " + errorMsg);
    }

    @Override
    public void exerciseDownloadCancelledByUser(Exercise cancelledExercise) {
        ModalDialogDisplayer.getDefault().displayError("download cancelled: " + cancelledExercise.getName());
    }

    @Override
    public void exerciseDownloadCompleted() {
        exerciseDownloader = null;
    }

    
    /**
     * Sends the current main project exercise back to the server for review.
     */
    @Override
    public void sendExerciseForReview(final JButton source) {
        String mainProjectPath = ProjectHandler.getMainProjectPath();
        if (!ProjectHandler.isExercise(mainProjectPath)) {
            ModalDialogDisplayer.getDefault().displayError("Main project isn't set or it isn't any known exercise. Please set exercise as main project and try again.");

            return;
        }

        LifecycleManager.getDefault().saveAll();

        try {
            source.setEnabled(false);
            this.sendButton = source;

            Exercise exercise = ProjectHandler.getExercise(mainProjectPath);

            this.uploader = new ExerciseUploader(exercise, this);
            this.uploader.sendExercise();

        } catch (Exception e) {
            source.setEnabled(true);
            this.uploader = null;
            ModalDialogDisplayer.getDefault().displayError(e);
        }

    }

    /**
     * Runs all tests in the current main project.
     */
    @Override
    public void runTests() {
        Project project = OpenProjects.getDefault().getMainProject();

        if (project != null) {
            ActionProvider a = project.getLookup().lookup(ActionProvider.class);

            a.invokeAction(ActionProvider.COMMAND_TEST, project.getLookup());
        } else {
            ModalDialogDisplayer.getDefault().displayError("Main project isn't set. Please set exercise as main project and try again.");

        }

    }

    
    /* Exercise upload */
    /**
     * Called by the ExerciseUploader when an exercise has been uploaded.
     * This method then extracts the link to the test results and gives it to
     * testResultHandler.
     */
    @Override
    public void exerciseUploadCompleted(Exercise exercise, InputStream response) {
        this.sendButton.setEnabled(true);
        this.sendButton = null;
        this.uploader = null;

        try {

            String returnMessage = StreamToString.inputStreamToString(response);

            int start = 0, end = 0;
            start = returnMessage.indexOf("<a href=\"") + 9;
            returnMessage = returnMessage.substring(start);
            end = returnMessage.indexOf("\">");
            returnMessage = returnMessage.substring(0, end);

            String jsonLink = returnMessage + ".json";

            this.testHandler = new TestResultHandler(jsonLink, this);
            this.testHandler.execute(exercise);
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError(e);
        }
    }

    @Override
    public void exerciseUploadFailed(String errorMessage) {
        this.sendButton.setEnabled(true);
        this.sendButton = null;
        this.uploader = null;

        ModalDialogDisplayer.getDefault().displayError(errorMessage);
    }

    
    /* Test results */
    @Override
    public void runComplete(TestResultHandler handler) {
        if (handler.getNumberOfFailures() == 0) {
            ModalDialogDisplayer.getDefault().displayHappyNotification("File upload complete and all tests passed!", "Well done!");

        } else {
            ModalDialogDisplayer.getDefault().showTestResultDialog(handler.getFailures());
        }

        this.testHandler = null;
    }

    @Override
    public void runFailed(String errorMessage) {
        this.testHandler = null;
        ModalDialogDisplayer.getDefault().displayError(errorMessage);
    }
}
