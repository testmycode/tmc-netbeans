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
    public ArrayList<Exercise> downloadable;
    public ArrayList<Exercise> updateable;

    public static LocalExerciseStatus get(List<Exercise> allExercises) {
        return new LocalExerciseStatus(CourseDb.getInstance(), ProjectMediator.getInstance(), allExercises);
    }

    private LocalExerciseStatus(CourseDb courseDb, ProjectMediator projectMediator, List<Exercise> allExercises) {
        open = new ArrayList<Exercise>();
        closed = new ArrayList<Exercise>();
        downloadable = new ArrayList<Exercise>();
        updateable = new ArrayList<Exercise>();

        for (Exercise ex : allExercises) {
            if (!ex.hasDeadlinePassed()) {
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
                boolean isDownloaded = proj != null;
                if (!isDownloaded) {
                    downloadable.add(ex);
                } else if (projectMediator.isProjectOpen(proj)) {
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

    public boolean thereIsSomethingToDownload() {
        return !downloadable.isEmpty() || !updateable.isEmpty();
    }

    @Override
    public String toString() {
        return
                "Open: " + open + "\n" +
                "Closed: " + closed + "\n" +
                "Downloadable: " + downloadable + "\n" +
                "Updateable: " + updateable + "\n";
    }

}
