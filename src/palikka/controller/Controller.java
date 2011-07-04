package palikka.controller;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JOptionPane;


import palikka.settings.PluginSettings;
import org.openide.DialogDescriptor;
import palikka.ui.swingPanels.PreferencesPanel;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ActionProvider;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import palikka.data.Course;
import palikka.data.Exercise;
import palikka.data.ExerciseCollection;
import palikka.settings.Settings;
import palikka.utilities.AdvancedDownloadFeature;
import palikka.utilities.exercise.ExerciseFilter;


import palikka.utilities.CourseAndExerciseInfo;
import palikka.utilities.FolderHelper;
import palikka.utilities.exercise.ExerciseDownloader;
import palikka.utilities.exercise.ExerciseUploader;
import palikka.utilities.exercise.IExerciseDownloadListener;
import palikka.utilities.json.updaters.IExerciseListUpdateListener;
import palikka.utilities.exercise.IExerciseUploadListener;
import palikka.utilities.json.updaters.JSONExerciseListUpdater;
import palikka.utilities.exercise.ExerciseLoader;
import palikka.utilities.ProjectHandler;
import palikka.utilities.ModalDialogDisplayer;
import palikka.utilities.exercise.ExerciseStatus;
import palikka.utilities.exercise.ITestResultListener;
import palikka.utilities.exercise.TestResultHandler;
import palikka.utilities.textio.StreamToString;
import palikka.utilities.zip.Unzipper;

/**
 *
 * @author jmturpei
 */
public class Controller implements IController, IExerciseListUpdateListener, IExerciseDownloadListener, IExerciseUploadListener, ITestResultListener {

    /**
     * A reference to the default controller.
     */
    private static Controller defaultController;
    /**
     * A reference to exerciseDownloader to keep it alive
     */
    private ExerciseDownloader exerciseDownloader;
    /**
     * A reference to exerciseUploader to keep it alive
     */
    private ExerciseUploader uploader;
    /**
     * A reference to JSONExerciseListUpdater to keep it alive
     */
    private JSONExerciseListUpdater exerciseListDownloader;
    private JButton sendButton;
    private TestResultHandler testHandler;

    /**
     * Returns the default instance of the controller.
     * @return 
     */
    public static IController getInstance() {
        if (defaultController == null) {
            defaultController = new Controller();

        }

        return defaultController;
    }

    /**
     * This method is invoked by getInstance() when no default controller is available.
     */
    private Controller() {
        uploader = null;
        sendButton = null;
    }

    /**
     * Creates and displays the preferences window.
     */
    @Override
    public void showPreferences() {


        Settings settings = PluginSettings.getSettings();

        PreferencesPanel panel = new PreferencesPanel(settings);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    if (event.getSource() == DialogDescriptor.OK_OPTION) {
                        PluginSettings.saveSettings();
                    } else {
                        PluginSettings.loadFromFile(); //this erases all changes which user didn't want to save.
                    }
                } catch (Exception e) {
                    ModalDialogDisplayer.getDefault().displayError(e);
                }
            }
        };

        DialogDescriptor descriptor = new DialogDescriptor(panel,
                "Preferences", true, NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.PLAIN_MESSAGE, listener);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setResizable(false);
        dialog.setVisible(true);
        panel.interruptCourseListUpdate();


    }

    /**
     * Method dowloads exercises that haven't expired and haven't already been downloaded.
     * It opens these exercises and the exercises that you have already downloaded that haven't 
     * expired onto project window.
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
     * @param collection 
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
     * exercise. This method then unzips the exercise and opens it.
     * @param downloadedExercise
     * @param fileContent 
     */
    @Override
    public void ExerciseDownloadCompleted(Exercise downloadedExercise, InputStream fileContent) {

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



        Unzipper uz = new Unzipper();

        try {
            uz.unZip(fileContent, FolderHelper.generatePath(downloadedExercise).getAbsolutePath());
            ExerciseLoader.open(downloadedExercise);
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError(e);
        }


        ExerciseStatus.resetStatus(downloadedExercise);
    }

    @Override
    public void ExerciseDownloadFailed(String errorMsg) {
        ModalDialogDisplayer.getDefault().displayError("download failed: " + errorMsg);
    }

    @Override
    public void ExerciseDownloadCancelledByUser(Exercise cancelledExercise) {
        ModalDialogDisplayer.getDefault().displayError("download cancelled: " + cancelledExercise.getName());
    }

    @Override
    public void ExercisedownloaderCompleted() {
        exerciseDownloader = null;
    }

    /**
     * This method is invoked to send an exercise set back to the server for review.
     */
    @Override
    public void send(final JButton source) {

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
     * This method is used to run all tests in the current main project.
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
     * This method then extracts the link to the testresults and gives it to
     * testResultHandler.
     * @param exercise
     * @param response 
     */
    @Override
    public void ExerciseUploadCompleted(Exercise exercise, InputStream response) {
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
    public void ExerciseUploadFailed(String errorMessage) {
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
