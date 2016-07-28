package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ExerciseUpdateOverwritingDecider;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;

import com.google.common.collect.ImmutableList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class UpdateExercisesAction implements ActionListener {

    private static final Logger log = Logger.getLogger(UpdateExercisesAction.class.getName());

    private List<Exercise> exercisesToUpdate;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogDisplayer;
    private TmcEventBus eventBus;

    public UpdateExercisesAction(List<Exercise> exercisesToUpdate) {
        this.exercisesToUpdate = exercisesToUpdate;
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        final AggregatingBgTaskListener<TmcProjectInfo> projectOpener = new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToUpdate.size(), new BgTaskListener<Collection<TmcProjectInfo>>() {
            @Override
            public void bgTaskReady(Collection<TmcProjectInfo> result) {
                result = new ArrayList<TmcProjectInfo>(result);

                // result may contain nulls since some downloads might fail
                while (result.remove(null)) {
                }

                projectMediator.scanForExternalChanges(result);

                // Open all at once. This is much faster.
                projectMediator.openProjects(result);
            }

            // Cancelled and failed are never called since we only call bgTaskReady below manually
            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
            }
        });

        for (final Exercise exercise : exercisesToUpdate) {
            final File projectDir = projectMediator.getProjectDirForExercise(exercise);
            eventBus.post(new InvokedEvent(exercise));

            Callable<List<Exercise>> downloadAndExtractExerciseTask = TmcCore.get().downloadOrUpdateExercises(ProgressObserver.NULL_OBSERVER, ImmutableList.of(exercise));
            BgTask.start("Downloading " + exercise.getName(), downloadAndExtractExerciseTask, new BgTaskListener<List<Exercise>>() {

                @Override
                public void bgTaskReady(List<Exercise> exercises) {
                    courseDb.exerciseDownloaded(exercise);
                    TmcProjectInfo project = projectMediator.tryGetProjectForExercise(exercise);
                    projectOpener.bgTaskReady(project);
                }

                @Override
                public void bgTaskCancelled() {
                    projectOpener.bgTaskReady(null);
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    projectOpener.bgTaskReady(null);
                    String msg = ServerErrorHelper.getServerExceptionMsg(ex);
                    dialogDisplayer.displayError("Failed to download updated exercises.\n" + msg, ex);
                }
            });
        }
    }

    public static class InvokedEvent implements TmcEvent {

        public final Exercise exercise;

        public InvokedEvent(Exercise exercise) {
            this.exercise = exercise;
        }
    }
}
