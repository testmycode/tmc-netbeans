/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.actions;

import com.google.common.base.Function;
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
import org.netbeans.api.project.ui.OpenProjects;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadAdaptiveExerciseAction")
@ActionRegistration(displayName = "#CTL_DownloadAdaptiveExerciseAction")
@ActionReference(path = "Menu/TM&C", position = -200)
@Messages("CTL_DownloadAdaptiveExerciseAction=DownloadAdaptiveExerciseAction")
public final class DownloadAdaptiveExerciseAction implements ActionListener {

    private static final Logger logger = Logger.getLogger(DownloadSolutionAction.class.getName());
    
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;
    
    public DownloadAdaptiveExerciseAction() {
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
        logger.log(Level.WARNING, "hey you pålk,gpodfk,pogdkf,g");
        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Exercise> ex = TmcCore.get().downloadAdaptiveExercise(observer);
        BgTask.start("hjhgjgj", ex, observer, new BgTaskListener<Exercise>() {
            @Override
            public void bgTaskReady(Exercise ex) {
                TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(ex);
                projectMediator.openProject(proj);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                logger.log(Level.SEVERE, "murks<ijmdoisafs");
            }
        });
    }
}
