package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Exercise;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Groups exercises by their statuses.
 */
public class LocalExerciseStatus {

    public ArrayList<Exercise> open;
    public ArrayList<Exercise> closed;
    public ArrayList<Exercise> downloadableUncompleted;
    public ArrayList<Exercise> downloadableCompleted;
    public ArrayList<Exercise> updateable;
    public ArrayList<Exercise> unlockable;

    public static LocalExerciseStatus get(List<Exercise> allExercises) {
        return new LocalExerciseStatus(CourseDb.getInstance(), ProjectMediator.getInstance(), allExercises);
    }

    private LocalExerciseStatus(CourseDb courseDb, ProjectMediator projectMediator, List<Exercise> allExercises) {
        open = new ArrayList<Exercise>();
        closed = new ArrayList<Exercise>();
        downloadableUncompleted = new ArrayList<Exercise>();
        downloadableCompleted = new ArrayList<Exercise>();
        updateable = new ArrayList<Exercise>();
        unlockable = new ArrayList<Exercise>();

        for (Exercise ex : allExercises) {
            if (!ex.hasDeadlinePassed()) {
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
                boolean isDownloaded = proj != null;
                if (courseDb.isUnlockable(ex)) {
                    unlockable.add(ex);
                } else if (!isDownloaded && !ex.isLocked()) {
                    if (ex.isCompleted()) {
                        downloadableCompleted.add(ex);
                    } else {
                        downloadableUncompleted.add(ex);
                    }
                } else if (isDownloaded && projectMediator.isProjectOpen(proj)) {
                    open.add(ex);
                } else {
                    closed.add(ex); // TODO: all projects may end up here if this is queried too early
                }

                String downloadedChecksum = courseDb.getDownloadedExerciseChecksum(ex.getKey());
                if (isDownloaded && ObjectUtils.notEqual(downloadedChecksum, ex.getChecksum())) {
                    updateable.add(ex);
                }
            }
        }
    }

    public boolean thereIsSomethingToDownload(boolean includeCompleted) {
        return !unlockable.isEmpty() ||
                !downloadableUncompleted.isEmpty() ||
                !updateable.isEmpty() ||
                (includeCompleted && !downloadableCompleted.isEmpty());
    }

    @Override
    public String toString() {
        return
                "Unlockable: " + unlockable + "\n" +
                "Open: " + open + "\n" +
                "Closed: " + closed + "\n" +
                "Downloadable uncompleted: " + downloadableUncompleted + "\n" +
                "Downloadable completed: " + downloadableCompleted + "\n" +
                "Updateable: " + updateable + "\n";
    }

}
