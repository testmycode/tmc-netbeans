package fi.helsinki.cs.tmc.ui;

import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * Emulates a selectable text label with a non-editable JTextField.
 */
public class SelectableText extends JTextArea {
    public SelectableText() {
        this.setEditable(false);
        this.setBackground(null); // Inherit from parent
        this.setBorder(null);
        this.setForeground(UIManager.getColor("Label.foreground"));
        this.setFont(UIManager.getFont("Label.font"));
    }

    public SelectableText(String text) {
        this();
        this.setText(text);
    }
}
