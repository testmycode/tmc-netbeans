
package fi.helsinki.cs.tmc.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;

/**
 * A clickable JLabel that looks like a link.
 * 
 * <p>
 * As of 2012-02-24 this is unused but left in since it may yet be useful.
 */
public class HyperlinkLabel extends JLabel {

    private static final Color DEFAULT_COLOR = Color.BLUE;
    private static final Font DEFAULT_FONT;
    
    static {
        Font font = new JLabel().getFont();
        Map<TextAttribute, Object> fontAttrs = new HashMap<TextAttribute, Object>();
        fontAttrs.putAll(font.getAttributes());
        fontAttrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        font = font.deriveFont(fontAttrs);
        
        DEFAULT_FONT = font;
    }
    
    
    private List<ActionListener> listeners;
    
    public HyperlinkLabel() {
        this("");
    }

    public HyperlinkLabel(String text) {
        super(text);
        this.setFont(DEFAULT_FONT);
        this.setForeground(DEFAULT_COLOR);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        this.listeners = new ArrayList<ActionListener>();
        
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fireActionEvent();
            }
        });
    }
    
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    protected void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "clicked");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
    
}
