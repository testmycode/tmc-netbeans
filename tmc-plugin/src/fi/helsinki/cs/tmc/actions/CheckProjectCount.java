package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckProjectCount {
    private final ProjectMediator projects;
    private final TmcNotificationDisplayer notifyer;
    private static final int DESIRED_COUNT = 50;

    public CheckProjectCount() {
        this.projects = ProjectMediator.getInstance();
        this.notifyer = TmcNotificationDisplayer.getDefault();        
    }
    
    public void checkAndNotifyIfOver() {
        if (this.getProjectCount() > DESIRED_COUNT) {
            this.notifyer().notify("Netbeans slow? Close some projects!", ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/ui/infobubble.png", false), "Right-click completed projects from the Projects -sidebar on the left and select 'Close project'.", null);
        }
    }
    
    public int getProjectCount() {
        return this.projects.getOpenProjects().size();
    }
    
    private TmcNotificationDisplayer notifyer() {
        return this.notifyer;
    }
}
