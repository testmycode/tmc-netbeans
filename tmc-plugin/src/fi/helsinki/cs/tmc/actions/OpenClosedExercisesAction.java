package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class OpenClosedExercisesAction {
    private final ProjectMediator projects;

    public OpenClosedExercisesAction() {
        this.projects = ProjectMediator.getInstance();
    }
    
    public void run(final List<Exercise> exercisesToOpen) {
        BgTask.start("Opening closed exercises", new Callable() {
            @Override
            public Void call() {
                List<Exercise> unopenedExercises = new ArrayList<>();
                for (Exercise ex : exercisesToOpen) {
                    TmcProjectInfo project = projects.tryGetProjectForExercise(ex);
                    if (project != null && !projects.isProjectOpen(project)) {
                        unopenedExercises.add(ex);
                    }
                }

                if (!unopenedExercises.isEmpty()) {
                    openAction(unopenedExercises);
                }
                
                return null;
            }
        });
    }
    
    private void openAction(final List<Exercise> exercises) {
        for (Exercise ex : exercises) {
            TmcProjectInfo project = projects.tryGetProjectForExercise(ex);
            if (project != null && !projects.isProjectOpen(project)) {
                projects.openProject(project);
            }
        }
    }
}
