/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.AggregatingBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.TmcSwingUtilities;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;



/**
 *
 * @author Piia Hartikka
 * 
 * 
 * Opin seuraavista:
 * 
 * - DownloadExercisesAction
 * Lataa kaikki kurssin tehtävät... Siitä voisi ottaa mallia yhden tehtävän lataamiseen.
 * 
 * - DownloadSolutionAction
 * Sisältää nappulaan liittyvää toiminnallisuutta, josta voisi ottaa mallia.
 * 
 */



@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadNextAdaptiveExerciseAction")
//@ActionRegistration(displayName = "#CTL_DownloadSolutionAction", lazy = false)
//@ActionReferences({@ActionReference(path = "Menu/TM&C", position = -35, separatorAfter = -30)})
//@NbBundle.Messages("CTL_DownloadSolutionAction=Download suggested &solution")
public class DownloadNextAdaptiveExerciseAction extends AbstractExerciseSensitiveAction {
   
    private static final Logger logger = Logger.getLogger(DownloadNextAdaptiveExerciseAction.class.getName());

    private ProjectMediator projectMediator;

    public DownloadNextAdaptiveExerciseAction() {
        this.projectMediator = ProjectMediator.getInstance();
    }
    
    
    // katsotaan, onko tarpeellinen
    // esimerkiksi silleen, että napin kuuntelija käynnistää toiminnallisuuden?
    // ja onhan sitten luokassa DownloadExerciseActionin run-metodilla jtn
    // toiminnallisuutta, jota en vielä ymmärtänyt
    public void run() {
        
    }
    
    
    // kopsattu pitkälti downloadexercisesactionista... soveltaen
    private void downloadExercise() {
        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Exercise> downloadAdaptiveExerciseTask = TmcCore.get().downloadAdaptiveExercise(observer);
        
        BgTask.start("Downloading adaptive exercise", downloadAdaptiveExerciseTask, observer, new BgTaskListener<Exercise>() {
            @Override
            public void bgTaskReady(Exercise result) {
                try {
                    logger.warning("res: " + result);
                    
                    // There is only one exercise given as parameter.
                    if (result == null) {
                        logger.log(Level.INFO, "Download task returned a null exercise");
                        return;
                    }
                    TmcProjectInfo proj = projectMediator.tryGetProjectForExercise(result);
                    
                    if (proj == null) {
                        throw new RuntimeException("Failed to open project for exercise " + result.getName());
                    }

                    // Need to invoke courseDb in swing thread to avoid races
//                    TmcSwingUtilities.ensureEdt(new Runnable() {
//                        @Override
//                        public void run() {
//                            courseDb.exerciseDownloaded(exercise);
//                        }
//                    });
                    //listener.bgTaskReady(proj);

                } catch (RuntimeException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            @Override
            public void bgTaskCancelled() {
                //listener.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                //listener.bgTaskFailed(ex);
            }
        });
    }

    @Override
    protected ProjectMediator getProjectMediator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected CourseDb getCourseDb() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void performAction(Node[] nodes) {
        
        // TÄHÄN SUORITETTAVA KOODI
        // elikkä:
        
        downloadExercise();
    }

    @Override
    public String getName() {
        return "Download new adaptive exercise";
    }
    
    private JMenuItem getOriginalMenuPresenter() {
        return super.getMenuPresenter();
    }
    
    
    // tarvitaanko tätä?
    public static class InvokedEvent implements TmcEvent {

        public final Exercise exercise;

        public InvokedEvent(Exercise exercise) {
            this.exercise = exercise;
        }
    }
    
    
    // tarvitaanko tätä? miten tämä toimii?
    private class ActionMenuItem extends JMenuItem implements DynamicMenuContent {

        public ActionMenuItem() {
            super(DownloadNextAdaptiveExerciseAction.this);
        }

        @Override
        public JComponent[] getMenuPresenters() {
            if (DownloadNextAdaptiveExerciseAction.this.isEnabled()) {
                return new JComponent[] {getOriginalMenuPresenter()};
            } else {
                return new JComponent[0];
            }
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] jcs) {
            return getMenuPresenters();
        }
    }

}
