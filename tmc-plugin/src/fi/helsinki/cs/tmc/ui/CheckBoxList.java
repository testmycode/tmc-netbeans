package fi.helsinki.cs.tmc.ui;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * A list of labeled checkboxes.
 *
 * <p>
 * Based on: http://www.devx.com/tips/Tip/5342
 * See also: http://stackoverflow.com/questions/19766/how-do-i-make-a-list-with-checkboxes-in-java-swing
 */
public class CheckBoxList extends JList {
    
    private List<ItemListener> itemListeners;

    public CheckBoxList() {
        this.itemListeners = new ArrayList<ItemListener>();
        this.setCellRenderer(new CellRenderer());
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index != -1) {
                    JCheckBox checkbox = (JCheckBox)getModel().getElementAt(index);
                    checkbox.setSelected(!checkbox.isSelected());
                    checkbox.addItemListener(itemEventForwarder);
                    repaint();
                }
            }
        });
    }
    
    private ItemListener itemEventForwarder = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            fireItemEvent(e);
        }
    };
    
    public void addItemListener(ItemListener listener) {
        itemListeners.add(listener);
    }
    
    protected void fireItemEvent(ItemEvent e) {
        for (ItemListener listener : itemListeners) {
            listener.itemStateChanged(e);
        }
    }

    public void addCheckbox(JCheckBox newCheckBox) {
        ListModel model = getModel();
        JCheckBox[] newData = new JCheckBox[model.getSize() + 1];
        for (int i = 0; i < model.getSize(); ++i) {
            newData[i] = (JCheckBox)model.getElementAt(i);
        }
        newData[newData.length - 1] = newCheckBox;
        setListData(newData);
    }

    public boolean isSelected(int i) {
        return ((JCheckBox)getModel().getElementAt(i)).isSelected();
    }
    
    public void setSelected(int i, boolean selected) {
        ((JCheckBox)getModel().getElementAt(i)).setSelected(selected);
        repaint();
    }
    
    public boolean isAnySelected() {
        for (int i = 0; i < getModel().getSize(); ++i) {
            if (isSelected(i)) {
                return true;
            }
        }
        return false;
    }

    protected class CellRenderer implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JCheckBox checkbox = (JCheckBox)value;
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(false);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : new EmptyBorder(1, 1, 1, 1));
            return checkbox;
        }
    }
}
