package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;

public class CheckForOneDrive {

    public static void run() {
        TmcSettings settings = TmcSettingsHolder.get();

        if (!settings.getTmcProjectDirectory().toAbsolutePath().toString().toLowerCase().contains("onedrive")) {
            return;
        }

        final ConvenientDialogDisplayer displayer = ConvenientDialogDisplayer.getDefault();

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        htmlBuilder.append("<h1>OneDrive does not work with the Test My Code plugin for Netbeans</h1>");
        htmlBuilder.append("<div>Many students have had a problem when they have their Netbeans project folder in OneDrive, which results in submissions failing on the server.<br>The problem can be solved by moving the project folder out of OneDrive as follows:</div>");
        htmlBuilder.append("<ol>");
        htmlBuilder.append("<li>In Netbeans, close all the exercises by right clicking them on the \"Projects\"-sidebar on the left and selecting \"Close project\".<br>You can select multiple exercises at once by pressing down the shift button on your keyboard and while pressing the button, selecting the first and the last exercise on the list.</li>");
        htmlBuilder.append("<li>Copy or move the \"NetbeansProjects\" folder to some path that does not contain the \"OneDrive\" folder.</li>");
        htmlBuilder.append("<li>Now you can change the Netbeans project folder to the path you selected on step 2 by selecting \"TMC\" -> \"Settings\".</li>");
        htmlBuilder.append("<li>Open the moved exercises by selecting \"File\" -> \"Open Project...\", and selecting the exercises from the new folder.</li>");
        htmlBuilder.append("</ol>");
        htmlBuilder.append("</body></html>");

        String errorMsg = htmlBuilder.toString();

        displayer.displayError(errorMsg);
    }
}
