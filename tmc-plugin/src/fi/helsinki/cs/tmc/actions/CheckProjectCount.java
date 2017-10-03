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
            this.notifyer().notify("Too many projects open!", ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/ui/infobubble.png", false), "Please close some projects to make Netbeans faster.", null);
        }
    }
    
    public int getProjectCount() {
        return this.projects.getOpenProjects().size();
    }
    
    private TmcNotificationDisplayer notifyer() {
        return this.notifyer;
    }
}
