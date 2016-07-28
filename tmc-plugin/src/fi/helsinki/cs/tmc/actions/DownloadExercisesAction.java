package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;

import com.google.common.collect.Lists;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.util.Exceptions;

/**
 * Downloads and opens the given exercises in the background.
 */
public class DownloadExercisesAction {

    private static final Logger logger = Logger.getLogger(DownloadExercisesAction.class.getName());

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;

    private List<Exercise> exercisesToDownload;

    public DownloadExercisesAction(List<Exercise> exercisesToOpen) {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();

        this.exercisesToDownload = exercisesToOpen;
    }

    public void run() {
        final AggregatingBgTaskListener<TmcProjectInfo> aggregator
                = new AggregatingBgTaskListener<TmcProjectInfo>(exercisesToDownload.size(), whenAllDownloadsFinished);

        for (final Exercise exercise : exercisesToDownload) {
            eventBus.post(new InvokedEvent(exercise));
            startDownloading(exercise, aggregator);
        }
    }

    private void startDownloading(final Exercise exercise, final BgTaskListener<TmcProjectInfo> listener) {
        Callable<List<Exercise>> downloadExercisesTask = TmcCore.get().downloadOrUpdateExercises(ProgressObserver.NULL_OBSERVER, Lists.newArrayList(exercise));

        BgTask.start("Downloading " + exercise.getName(), downloadExercisesTask, new BgTaskListener<List<Exercise>>() {
            @Override
            public void bgTaskReady(List<Exercise> result) {
                try {
                    logger.warning("res: " + result);
                    // There is only one exercise given as parameter.
                    TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(result.get(0));

                    if (proj == null) {
                        throw new RuntimeException("Failed to open project for exercise " + exercise.getName());
                    }

                    // Need to invoke courseDb in swing thread to avoid races
                    // java.lang.Error: Cannot call invokeAndWait from the event dispatcher thread
                    TmcSwingUtilities.ensureEdt(new Runnable() {
                        @Override
                        public void run() {
                            courseDb.exerciseDownloaded(exercise);
                        }
                    });
                    listener.bgTaskReady(proj);

                } catch (RuntimeException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            @Override
            public void bgTaskCancelled() {
                listener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                listener.bgTaskFailed(ex);
            }
        });
    }

    private BgTaskListener<Collection<TmcProjectInfo>> whenAllDownloadsFinished = new BgTaskListener<Collection<TmcProjectInfo>>() {
        @Override
        public void bgTaskReady(Collection<TmcProjectInfo> projects) {
            projectMediator.openProjects(projects);
        }

        @Override
        public void bgTaskCancelled() {
        }

        @Override
        public void bgTaskFailed(Throwable ex) {
            logger.log(Level.INFO, "Failed to download exercise file.", ex);
            dialogs.displayError("Failed to download exercises.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
        }
    };

    public static class InvokedEvent implements TmcEvent {

        public final Exercise exercise;

        public InvokedEvent(Exercise exercise) {
            this.exercise = exercise;
        }
    }
}
