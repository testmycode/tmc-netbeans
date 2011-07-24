package fi.helsinki.cs.tmc.utilities.exercise;

import java.io.File;
import fi.helsinki.cs.tmc.utilities.http.FileUploaderAsync;
import fi.helsinki.cs.tmc.utilities.http.IUploadListener;
import java.io.IOException;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.settings.LegacyPluginSettings;
import fi.helsinki.cs.tmc.settings.LegacySettings;
import fi.helsinki.cs.tmc.utilities.FolderHelper;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectZipper;

/**
 * This class is used to send a single exercise to the server for review.
 * @author knordman
 */
public class ExerciseUploader implements IUploadListener {

    private FileUploaderAsync uploader;
    private IExerciseUploadListener listener;
    private Exercise exercise;

    /**
     * Constructor
     * @param exercise The exercise we want to send
     * @param uploadListener Interface that listens to this ExerciseUploader
     * @throws NullPointerException When fails to create an uploader
     */
    public ExerciseUploader(Exercise exercise, IExerciseUploadListener uploadListener) throws NullPointerException {

        this.uploader = new FileUploaderAsync(exercise.getReturnAddress(), this);
        this.uploader.setTimeout(LegacyPluginSettings.getSettings().getExerciseUploadTimeout());
        this.listener = uploadListener;
        this.exercise = exercise;

    }

    /**
     * The method sends whatever exercise was given at the time the object
     * was contructed.
     */
    public void sendExercise() {
        LegacySettings settings = LegacyPluginSettings.getSettings();

        if (!settings.isValid()) {
            listener.exerciseUploadFailed("Student id not set. Check preferences before sending.");
            return;
        }

        try {
            File path = FolderHelper.searchSrcFolder(exercise);

            if (path == null) {
                listener.exerciseUploadFailed("Couldn't locate source folder. Unable to send exercise");
                return;
            }

            NbProjectZipper zipper = new NbProjectZipper();
            byte[] fileContent = zipper.zip(path);

            uploader.AddFile(fileContent, "exercise.zip", "exercise_return[tmp_file]");
            uploader.AddStringKeyValuePart("exercise_return[student_id]", settings.getStudentID());


            uploader.send("Sending exercise " + exercise.getName());
        } catch (IOException ioex) {  //Return the occurring IOException instead
            listener.exerciseUploadFailed(ioex.getMessage());
        }
    }

    /**
     * Called when an upload is complete
     * @param source 
     */
    @Override
    public void uploadCompleted(FileUploaderAsync source) {
        listener.exerciseUploadCompleted(this.exercise, source.getResponse());
    }

    /**
     * Called by the uploader when an upload fails
     * @param source 
     */
    @Override
    public void uploadFailed(FileUploaderAsync source) {
        listener.exerciseUploadFailed(this.uploader.getErrorMsg());
    }
}
