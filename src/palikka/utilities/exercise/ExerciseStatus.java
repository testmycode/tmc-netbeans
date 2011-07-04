package palikka.utilities.exercise;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import palikka.data.Exercise;
import palikka.utilities.FolderHelper;
import palikka.utilities.PalikkaConstants;

/**
 * Used to store, write and read the status of a single exercise from disk.
 * This is used to remember if an exercise has passed all tests, some tests or
 * has not been sent yet.
 * @author jmturpei
 */
public class ExerciseStatus implements Serializable {

    /**
     * Stores the status of an exercise
     */
    private Status status;

    private ExerciseStatus() {
        status = Status.NotSend;
    }

    public enum Status {

        AllTestsPassed,
        SendAndSomeTestsFailed,
        NotSend
    }

    /**
     * Reset the status for the given exercise. So delete the status file.
     * @param exercise 
     */
    public static void resetStatus(Exercise exercise) {
        File statusFile = getStatusFilePath(exercise);
        if (statusFile == null) {
            return;
        }

        if (statusFile.exists()) {
            statusFile.delete();
        }
    }

    /**
     * Find out where the status file of the give exercise is.
     * @param exercise
     * @return The status file
     */
    private static File getStatusFilePath(Exercise exercise) {
        if (exercise == null) {
            throw new NullPointerException("exercise is null");
        }

        File projectFolder = FolderHelper.searchNbProject(exercise);
        if (projectFolder == null) {
            return null;
        }

        String statusFilePath = projectFolder.getAbsolutePath() + PalikkaConstants.fileSeparator + "exerciseStatus.bin";

        return new File(statusFilePath);
    }

    /**
     * Get the status for the given exercise
     * @param exercise
     * @return The status
     * @throws Exception If no exercise was provided or failed to read the status
     */
    public static ExerciseStatus getStatus(Exercise exercise) throws Exception {
        if (exercise == null) {
            throw new NullPointerException("exercise is null");
        }

        ExerciseStatus status;

        File statusFile = getStatusFilePath(exercise);
        if (statusFile == null || !statusFile.exists()) {
            return new ExerciseStatus();
        }

        try {
            FileInputStream fis = new FileInputStream(statusFile);
            ObjectInputStream objIn = new ObjectInputStream(fis);

            status = (ExerciseStatus) objIn.readObject();
            objIn.close();
            fis.close();
        } catch (Exception e) {
            throw new Exception("Couldn't read exercise status from file");
        }

        return status;

    }

    /**
     * Write the given status for the given exercise to disk
     * @param status
     * @param exercise
     * @throws Exception 
     */
    public static void writeToFile(ExerciseStatus status, Exercise exercise) throws Exception {
        try {
            FileOutputStream out = new FileOutputStream(getStatusFilePath(exercise));
            ObjectOutputStream obout = new ObjectOutputStream(out);

            obout.writeObject(status);
            obout.close();
            out.close();
        } catch (Exception e) {
            throw new Exception("Couldn't write exercise status to file:" + e.getMessage());
        }

    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
