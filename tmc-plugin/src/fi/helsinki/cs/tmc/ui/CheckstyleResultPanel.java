package fi.helsinki.cs.tmc.ui;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleResult;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class CheckstyleResultPanel extends JPanel {

    public CheckstyleResultPanel() {
        this.setLayout(new GridLayout(0, 1));
    }

    public void setCheckstyleResults(CheckstyleResult result) {

        this.removeAll();
        JTextArea area = new JTextArea();
        area.setEditable(false);
        for (Entry<File, List<AuditEvent>> entry : result.getResults().entrySet()) {

            area.append("File: " + entry.getKey().getName() + ", errors: " + entry.getValue().size() + "\n");

            for (AuditEvent event : entry.getValue()) {
                area.append("  " +event.getMessage() + "\n");
            }
            area.append("\n");
        }
        this.add(area);
    }
}
