package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

public class TmcModuleInstall extends ModuleInstall {
    private static final String PREF_FIRST_RUN = "firstRun";
    
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    
    @Override
    public void restored() {
        serverAccess = ServerAccess.getDefault();
        courseDb = CourseDb.getInstance();
        
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                Preferences prefs = NbPreferences.forModule(TmcModuleInstall.class);
                
                boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
                if (isFirstRun) {
                    doFirstRun();
                    prefs.putBoolean(PREF_FIRST_RUN, false);
                } else {
                    new CheckForNewExercises(new OpenExercisesAction()).actionPerformed(null);
                }
            }
        });
    }
    
    private void doFirstRun() {
        // If the tailoring gives us server settings
        // and we're able to download a course list
        // and there is exactly one course available
        // and it is not empty
        // then we make it the default course and ask to open exercises
        // else we show the usual welcome dialog asking to open settings.
        if (serverAccess.hasEnoughSettings()) {
            serverAccess.startDownloadingCourseList(new BgTaskListener<CourseList>() {
                @Override
                public void bgTaskReady(CourseList result) {
                    courseDb.setAvailableCourses(result);
                    if (result.size() == 1 && result.get(0).getExercises().size() > 0) {
                        courseDb.setCurrentCourseName(result.get(0).getName());
                        showOpenNowWelcomeDialog();
                    } else {
                        showWelcomeDialog();
                    }
                }

                @Override
                public void bgTaskCancelled() {
                    showWelcomeDialog();
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    showWelcomeDialog();
                }
            });
        } else {
            showWelcomeDialog();
        }
    }
    
    private void showWelcomeDialog() {
        String msg = SelectedTailoring.get().getFirstRunMessage();
        String title = "TMC installed";
        
        DialogDescriptor dd = new DialogDescriptor(msg, title);
        dd.setModal(true);
        dd.setMessageType(DialogDescriptor.INFORMATION_MESSAGE);
        dd.setOptions(new Object[] { DialogDescriptor.OK_OPTION });
        

        dd.setButtonListener(wrapActionInInvokeLater(new ShowSettingsAction()));
        
        DialogDisplayer.getDefault().notifyLater(dd);
    }
    
    /*
     * We need to wrap the showing of the settings window in invokeLater.
     * Otherwise the settings window may get the welcome dialog as its
     * parent, which leads to funky window focus problems such as
     * the settings window dropping behind the NetBeans window after
     * an error dialog from it is closed.
     */
    private ActionListener wrapActionInInvokeLater(final ActionListener a) {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        a.actionPerformed(e);
                    }
                });
            }
        };
    }
    
    private void showOpenNowWelcomeDialog() {
        String msg =
                "Test My Code (TMC) installed.\n" +
                courseDb.getCurrentCourse().getExercises().size() +
                " exercises available.\n" +
                "Open them?";
        String title = "TMC installed";
        
        DialogDescriptor dd = new DialogDescriptor(msg, title);
        dd.setModal(true);
        dd.setMessageType(DialogDescriptor.INFORMATION_MESSAGE);
        dd.setOptions(new Object[] { DialogDescriptor.YES_OPTION, DialogDescriptor.NO_OPTION });
        
        dd.setButtonListener(wrapActionInInvokeLater(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (e.getSource() == DialogDescriptor.YES_OPTION) {
                    new OpenExercisesAction().actionPerformed(e);
                }
            }
        }));
        
        DialogDisplayer.getDefault().notifyLater(dd);
    }
}
