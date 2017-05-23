package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadAdaptiveExerciseAction")
@ActionRegistration(displayName = "#CTL_DownloadAdaptiveExerciseAction")
@ActionReference(path = "Menu/TM&C", position = -200)
@Messages("CTL_DownloadAdaptiveExerciseAction=DownloadAdaptiveExerciseAction")
public final class DownloadAdaptiveExerciseAction implements ActionListener {

    private static final Logger logger = Logger.getLogger(DownloadAdaptiveExerciseAction.class.getName());
    
    private final ProjectMediator projectMediator;
    private final ConvenientDialogDisplayer dialogs;
    private final TmcEventBus eventBus;
    
    public DownloadAdaptiveExerciseAction() {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        logger.log(Level.WARNING, "Init adaptive exercise downloading");
        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Exercise> ex = TmcCore.get().downloadAdaptiveExercise(observer);
        BgTask.start("Downloading new adaptive exercise...", ex, observer, new BgTaskListener<Exercise>() {
            @Override
            public void bgTaskReady(Exercise ex) {
                if (ex == null) {
                    dialogs.displayMessage("No adaptive exercises available.");
                    return;
                }
                dialogs.displayMessage("A new adaptive exercise downloaded.");
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
                projectMediator.openProject(proj);
            }

            @Override
            public void bgTaskCancelled() {
                logger.log(Level.WARNING, "Adaptive exercise download cancelled");
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.SEVERE, "Something went wrong: {0}", ex.toString());
            }
        });
    }
}
