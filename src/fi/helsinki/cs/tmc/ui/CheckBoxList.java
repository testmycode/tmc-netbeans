package fi.helsinki.cs.tmc.ui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    public CheckBoxList() {
        this.setCellRenderer(new CellRenderer());
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index != -1) {
                    JCheckBox checkbox = (JCheckBox)getModel().getElementAt(index);
                    checkbox.setSelected(!checkbox.isSelected());
                    repaint();
                }
            }
        });
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

    boolean isSelected(int i) {
        return ((JCheckBox)getModel().getElementAt(i)).isSelected();
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
