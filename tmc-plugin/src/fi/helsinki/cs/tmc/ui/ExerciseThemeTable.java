package fi.helsinki.cs.tmc.ui;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class ExerciseThemeTable extends JTable implements Iterable<JCheckBox> {

    private List<JCheckBox> boxlist;
    private List<ItemListener> itemListeners;

    private final String[] HEADERS = {"Exercise", "Theme"};
    private final int COLUMN_EXERCISE = 0;
    private final int COLUMN_THEME = 1;

    private static final Logger logger = Logger.getLogger(ExerciseThemeTable.class.getName());

    public ExerciseThemeTable() {
        boxlist = new ArrayList<JCheckBox>();
        this.itemListeners = new ArrayList<ItemListener>();
        this.setDefaultRenderer(Object.class, new CellRenderer());
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                if (index != -1 && column == COLUMN_EXERCISE) {
                    JCheckBox checkbox = getElement(index);
                    if (ExerciseThemeTable.this.isEnabled() && checkbox.isEnabled()) {
                        checkbox.setSelected(!checkbox.isSelected());
                    }
                    repaint();
                }
            }
        });
    }

    public void sort() {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(getModel());
        setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(COLUMN_THEME, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(COLUMN_EXERCISE, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private ItemListener itemEventForwarder = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            fireItemEvent(e);
        }
    };

    private PropertyChangeListener checkBoxPropChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            repaint();
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

    public boolean isSelected(int i) {
        return getElement(i).isSelected();
    }

    public void setSelected(int i, boolean selected) {
        getElement(i).setSelected(selected);
    }

    public boolean isAnySelected() {
        for (int i = 0; i < getRowCount(); ++i) {
            if (isSelected(i)) {
                return true;
            }
        }
        return false;
    }

    public JCheckBox getElement(int i) {
        return (JCheckBox)getValueAt(i, 0);
    }

    @Override
    public Iterator<JCheckBox> iterator() {
        return new Iterator<JCheckBox>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < getRowCount();
            }

            @Override
            public JCheckBox next() {
                JCheckBox cb = getElement(i);
                i++;
                return cb;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void setRows(Object[][] rows) {
        JTable newTable = new JTable(rows, HEADERS);
        this.setModel(newTable.getModel());
        for (int i = 0; i < rows.length; i++) {
            addBox((JCheckBox) rows[i][0]);
        }
        sort();
    }

    public void setRow(JCheckBox newCheckBox, String theme) {
        TableModel model = getModel();
        Object[][] newData = new Object[model.getRowCount() + 1][model.getColumnCount()];
        for (int i = 0; i < model.getRowCount(); ++i) {
            newData[i][0] = model.getValueAt(i, 0);
            newData[i][1] = model.getValueAt(i, 1);
        }
        Object[] newRow = {newCheckBox, theme};
        newData[newData.length - 1] = newRow;
        JTable newTable = new JTable(newData, HEADERS);
        this.setModel(newTable.getModel());
        addBox(newCheckBox);
    }

    private void addBox(JCheckBox box) {
        boxlist.add(box);
        box.addItemListener(itemEventForwarder);
        box.addPropertyChangeListener(checkBoxPropChangeListener);
    }

    protected class CellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof JCheckBox) {
                JCheckBox checkbox = (JCheckBox)value;
                checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
                checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
                checkbox.setFont(getFont());
                checkbox.setFocusPainted(false);
                checkbox.setBorderPainted(false);
                checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : new EmptyBorder(1, 1, 1, 1));
                return checkbox;
            }
            return new JLabel((String)value);
        }
    }
}
