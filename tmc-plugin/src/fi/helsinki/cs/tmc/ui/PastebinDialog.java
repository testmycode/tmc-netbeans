/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.ui;

import static fi.helsinki.cs.tmc.ui.Boxer.hbox;
import static fi.helsinki.cs.tmc.ui.Boxer.hglue;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.openide.awt.HtmlBrowser;

/**
 *
 * @author kviiri
 */
public class PastebinDialog extends JDialog {

    private static final Logger log = Logger.getLogger(PastebinDialog.class.getName());

    public PastebinDialog(final String pasteUrl) {
        setTitle("Pastebin notification");
        
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);
        
        getContentPane().add(leftAligned(new JLabel("Code submitted to TMC pastebin.")));
        addVSpace(8);
        JButton viewPasteButton = new JButton(new AbstractAction("View paste") {
            @Override
                public void actionPerformed(ActionEvent ev) {
                    try {
                        HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(pasteUrl));
                    } catch (Exception ex) {
                        ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                    }
                }
        });
        
        getContentPane().add(leftAligned(viewPasteButton));
        addVSpace(8);
        
        JTextField pasteUrlField = new JTextField(pasteUrl);
        pasteUrlField.setEditable(false);
        getContentPane().add(leftAligned(pasteUrlField));
        
        addVSpace(8);
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(hbox(hglue(), okButton));
        
        pack();
        
        

    }
    //brutishly borrowed from SuccessfulSubmissionDialog
    private void addVSpace(int height) {
        add(Box.createVerticalStrut(height));
    }
    
    private Box leftAligned(Component component) {
        return hbox(component, hglue());
    }
}
