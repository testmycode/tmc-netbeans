package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.DownloadAdaptiveExerciseAction;
import static fi.helsinki.cs.tmc.ui.Boxer.*;

import org.openide.awt.HtmlBrowser;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AdaptiveExerciseAfterGroupDialog extends JDialog {

    private static final Logger log = Logger.getLogger(AdaptiveExerciseAfterGroupDialog.class.getName());

    private JButton okButton;
    private JCheckBox downloadNextExerciseButton;

    public AdaptiveExerciseAfterGroupDialog() {
        this.setTitle("Adaptive exercise");

        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);

        addHeadlineLabel();
        addVSpace(20);
        addDownloadBox();
        addVSpace(20);
        addInfoButton();
        addVSpace(20);
        addOkButton();

        pack();
        setSize(400, 300);
        setModalityType(Dialog.ModalityType.MODELESS);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void addVSpace(int height) {
        add(Box.createVerticalStrut(height));
    }

    private Box leftAligned(Component component) {
        return hbox(component, hglue());
    }

    private void addDownloadBox() {
        downloadNextExerciseButton =
                    new JCheckBox("Check and download adaptive exercise");
        downloadNextExerciseButton.setSelected(true);
        getContentPane().add(leftAligned(downloadNextExerciseButton));
    }

    private void addHeadlineLabel() {
        JLabel headline = new JLabel("Tehtäväryhmä suoritettu!");

        Font font = headline.getFont();
        font = font.deriveFont(Font.BOLD, font.getSize2D() * 1.2f);
        headline.setFont(font);
        headline.setIcon(ConvenientDialogDisplayer.getDefault().getSmileyIcon());

        getContentPane().add(leftAligned(headline));
    }

    private void addOkButton() {
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (downloadNextExerciseButton.isSelected()) {
                    new DownloadAdaptiveExerciseAction().actionPerformed(null);
                }
                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(leftAligned(okButton));
    }

    private void addInfoButton() {
        final String infoUrl = "https://mooc.fi/adaptinfo";
        JButton infoButton = new JButton(new AbstractAction("More info about adaptive exercises") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                try {
                    HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(infoUrl));
                } catch (Exception ex) {
                    ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                }
            }
        });
        getContentPane().add(leftAligned(infoButton));
    }
}
