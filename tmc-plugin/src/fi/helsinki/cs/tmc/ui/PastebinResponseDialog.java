package fi.helsinki.cs.tmc.ui;

import static fi.helsinki.cs.tmc.ui.Boxer.hbox;
import static fi.helsinki.cs.tmc.ui.Boxer.hglue;

import fi.helsinki.cs.tmc.utilities.BrowserOpener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

public class PastebinResponseDialog extends JDialog {

    private static final Logger log = Logger.getLogger(PastebinResponseDialog.class.getName());

    public PastebinResponseDialog(final String pasteUrl) {
        setTitle("Pastebin notification");

        // Set location according to screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(screenSize.width / 2 - (this.getWidth() / 2), screenSize.height / 2 - (this.getHeight() / 2));

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
                    BrowserOpener.openUrl(URI.create(pasteUrl));
                } catch (URISyntaxException | IOException ex) {
                    ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                }
            }
        });

        getContentPane().add(leftAligned(viewPasteButton));
        addVSpace(8);

        final JTextField pasteUrlField = new JTextField(pasteUrl);
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

        JButton copyToClipboardButton = new JButton("Copy to clipboard");
        copyToClipboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(pasteUrl);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, stringSelection);
            }
        });
        getContentPane().add(hbox(hglue(), copyToClipboardButton));

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
