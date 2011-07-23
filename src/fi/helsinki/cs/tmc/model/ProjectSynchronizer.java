package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

/**
 * Is able to download exercises from the server, unzip them
 * and open them as projects.
 */
public class ProjectSynchronizer {
    private static ProjectSynchronizer instance;

    public static ProjectSynchronizer getInstance() {
        if (instance == null) {
            instance = new ProjectSynchronizer(ServerAccess.getDefault(), ProjectMediator.getInstance());
        }
        return instance;
    }
    
    private ServerAccess serverAccess;
    private ProjectMediator projectMediator;
    
    public ProjectSynchronizer(ServerAccess serverAccess, ProjectMediator projectMediator) {
        this.serverAccess = serverAccess;
        this.projectMediator = projectMediator;
    }
    
    public void startDownloadingExercise(Exercise ex, BgTaskListener<TmcProjectInfo> listener) {
        throw new UnsupportedOperationException("TODO");
    }
}
