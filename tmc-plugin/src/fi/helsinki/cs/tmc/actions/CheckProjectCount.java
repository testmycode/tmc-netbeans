package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;

import javax.swing.ImageIcon;

import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;

public class CheckProjectCount {
    private final ProjectMediator projects;
    private final TmcNotificationDisplayer notifyer;
    private final ConvenientDialogDisplayer displayer;
    private static final int BALLOON_LIMIT = 20;
    private static final int POPUP_LIMIT = 50;

    public CheckProjectCount() {
        this.projects = ProjectMediator.getInstance();
        this.notifyer = TmcNotificationDisplayer.getDefault();        
        this.displayer = new ConvenientDialogDisplayer();
    }
    
    public void checkAndNotifyIfOver() {
        if (this.getProjectCount() >= POPUP_LIMIT) {
            this.displayPopup();
        } else if (this.getProjectCount() >= BALLOON_LIMIT) {
            this.displayBalloon();
        }
    }
    
    public int getProjectCount() {
        return this.projects.getOpenProjects().size();
    }
    
    private void displayPopup() {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        htmlBuilder.append("<h1>Too many projects open!</h1>");
        htmlBuilder.append("<div>Netbeans does background processing on open projects. When there's many projects open, Netbeans is slowed down significantly.<br>Please close some projects to make Netbeans faster. Instructions for closing projects:</div>");
        htmlBuilder.append("<ol>");
        htmlBuilder.append("<li>Right click the project you want to close on the \"Projects\"-sidebar on the left.</li>");
        htmlBuilder.append("<li>Select \"Close project\".</li>");
        htmlBuilder.append("<li>You can select multiple exercises at once by pressing down the shift button on your keyboard.<br>While pressing shift, select the first and the last exercise of the ones you want to close.</li>");
        htmlBuilder.append("</ol>");
        htmlBuilder.append("</body></html>");
        String errorMsg = htmlBuilder.toString();
        this.displayer.showDialog(errorMsg, NotifyDescriptor.WARNING_MESSAGE, "/fi/helsinki/cs/tmc/ui/close_projects.png");
    }

    private void displayBalloon() {
        final String header = "Netbeans slow? Close some projects!";
        final ImageIcon icon = ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/ui/infobubble.png", false);
        final String info = "Right-click completed projects from the Projects -sidebar on the left and select 'Close project'.";
        this.notifyer.notify(header, icon, info, null);
    }

}
