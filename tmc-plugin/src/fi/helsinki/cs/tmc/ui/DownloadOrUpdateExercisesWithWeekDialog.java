package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.DownloadAdaptiveExerciseAction;
import fi.helsinki.cs.tmc.actions.DownloadExercisesAction;
import fi.helsinki.cs.tmc.actions.UnlockExercisesAction;
import fi.helsinki.cs.tmc.actions.UpdateExercisesAction;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.model.CourseDb;

import org.openide.windows.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

public class DownloadOrUpdateExercisesWithWeekDialog extends JDialog {

    public static void display(List<Exercise> unlockable, List<Exercise> downloadable, List<Exercise> updateable) {
        DownloadOrUpdateExercisesWithWeekDialog dialog = new DownloadOrUpdateExercisesWithWeekDialog(unlockable, downloadable, updateable);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    boolean haveUnlockables;

    private List<JCheckBox> unlockableCheckboxes;
    private HashMap<JCheckBox, Exercise> checkBoxToExercise;

    private boolean selectAllButtonIsDeselecting;
    private CourseDb courseDb;

    private DownloadOrUpdateExercisesWithWeekDialog(List<Exercise> unlockable, List<Exercise> downloadable, List<Exercise> updateable) {
        super(WindowManager.getDefault().getMainWindow(), true);
        initComponents();

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        haveUnlockables = !unlockable.isEmpty();
        if (!haveUnlockables) {
            remove(unlockCheckbox);
        }
        if (downloadable.isEmpty() && unlockable.isEmpty()) {
            remove(downloadableLabel);
            remove(downloadTable);
            setTitle("Update exercises");
            downloadButton.setText("Update");
        }
        if (updateable.isEmpty()) {
            remove(updateableLabel);
            remove(jScrollPane1);
            setTitle("Download exercises");
            downloadButton.setText("Download");
        }
        courseDb = CourseDb.getInstance();
        if (courseDb.getAdaptiveWeek() == -1) {
            remove(downloadAdaptiveExButton);
        }

        int downloadTableSize = 0;
        for (Exercise ex : downloadable) {
            if (!ex.isAdaptive()) {
                downloadTableSize++;
            }
        }
        Object[][] downloadRows = new Object[downloadTableSize][2];

        int updateTableSize = updateable.size();
        Object[][] updateRows = new Object[updateTableSize][2];

        checkBoxToExercise = new HashMap<JCheckBox, Exercise>();
        for (int i = 0; i < downloadTableSize; i++) {
            Exercise exercise = downloadable.get(i);
            String text = exercise.getName();
            if (exercise.isCompleted()) {
                text += " (completed)";
            }

            if (!exercise.isAdaptive()) {
                JCheckBox cb = new JCheckBox(text, true);
                int week = exercise.getWeek();
                checkBoxToExercise.put(cb, exercise);
                downloadRows[i][0] = cb;
                downloadRows[i][1] = "week" + week;
            }
        }
        ((ExerciseWeekTable)downloadTableComponent).setRows(downloadRows);

        unlockableCheckboxes = new ArrayList<JCheckBox>();
        for (int i = 0; i < unlockable.size(); i++) {
            Exercise exercise = unlockable.get(i);
            String desc;
            if (exercise.getDeadlineDescription() != null) {
                desc = "unlockable; deadline: " + exercise.getDeadlineDescription();
            } else {
                desc = "unlockable";
            }
            JCheckBox cb = new JCheckBox(exercise.getName() + " (" + desc + ")", true);
            int week = exercise.getWeek();
            unlockableCheckboxes.add(cb);
            checkBoxToExercise.put(cb, exercise);
            ((ExerciseWeekTable)downloadTableComponent).setRow(cb, "week" + week);
        }

        for (int i = 0; i < updateTableSize; i++) {
            Exercise exercise = updateable.get(i);
            String text = exercise.getName();
            if (exercise.isCompleted()) {
                text += " (completed)";
            }
            JCheckBox cb = new JCheckBox(text, true);
            int week = exercise.getWeek();
            checkBoxToExercise.put(cb, exercise);
            updateRows[i][0] = cb;
            updateRows[i][1] = "week" + week;
        }
        ((ExerciseWeekTable)updateTableComponent).setRows(updateRows);
        ((ExerciseWeekTable)downloadTableComponent).addItemListener(updateSelectAllButtonStateListener);
        ((ExerciseWeekTable)updateTableComponent).addItemListener(updateSelectAllButtonStateListener);

        updateSelectAllButtonState();
        pack();
    }

    private ItemListener updateSelectAllButtonStateListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            updateSelectAllButtonState();
        }
    };

    private boolean isUnlockable(int index) {
        return unlockableCheckboxes.contains(((ExerciseWeekTable)downloadTableComponent).getElement(index));
    }

    private void updateSelectAllButtonState() {
        if (((ExerciseWeekTable)downloadTableComponent).isAnySelected() || ((ExerciseWeekTable)updateTableComponent).isAnySelected()) {
            selectAllButtonIsDeselecting = true;
            selectAllButton.setText("Unselect all");
        } else {
            selectAllButtonIsDeselecting = false;
            selectAllButton.setText("Select all");
        }
    }

    private void doDownloadAndUpdate(List<Exercise> toDownload, List<Exercise> toUpdate) {
        new DownloadExercisesAction(toDownload).run();
        new UpdateExercisesAction(toUpdate).run();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        downloadableLabel = new javax.swing.JLabel();
        updateableLabel = new javax.swing.JLabel();
        downloadButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        selectAllButton = new javax.swing.JButton();
        unlockCheckbox = new javax.swing.JCheckBox();
        downloadTable = new javax.swing.JScrollPane();
        downloadTableComponent = new fi.helsinki.cs.tmc.ui.ExerciseWeekTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        updateTableComponent = new fi.helsinki.cs.tmc.ui.ExerciseWeekTable();
        downloadAdaptiveExButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.title")); // NOI18N

        downloadableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.downloadableLabel.text")); // NOI18N

        updateableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.updateableLabel.text")); // NOI18N

        downloadButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.downloadButton.text")); // NOI18N
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        closeButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        selectAllButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.selectAllButton.text")); // NOI18N
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllButtonActionPerformed(evt);
            }
        });

        unlockCheckbox.setSelected(true);
        unlockCheckbox.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.unlockCheckbox.text")); // NOI18N
        unlockCheckbox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                unlockCheckboxItemStateChanged(evt);
            }
        });

        downloadTableComponent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        downloadTable.setViewportView(downloadTableComponent);

        updateTableComponent.setAutoCreateRowSorter(true);
        updateTableComponent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(updateTableComponent);

        downloadAdaptiveExButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithWeekDialog.class, "DownloadOrUpdateExercisesWithWeekDialog.downloadAdaptiveExButton.text")); // NOI18N
        downloadAdaptiveExButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadAdaptiveExButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(downloadTable)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(unlockCheckbox)
                            .addComponent(downloadableLabel)
                            .addComponent(updateableLabel))
                        .addGap(0, 294, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(selectAllButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(downloadAdaptiveExButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downloadButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(closeButton)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(downloadableLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(unlockCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(downloadTable, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateableLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton)
                    .addComponent(downloadButton)
                    .addComponent(downloadAdaptiveExButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
        final List<Exercise> toDownload = new ArrayList<Exercise>();
        for (JCheckBox cb : (ExerciseWeekTable)downloadTableComponent) {
            if (cb.isSelected()) {
                toDownload.add(checkBoxToExercise.get(cb));
            }
        }

        final List<Exercise> toUpdate = new ArrayList<Exercise>();
        for (JCheckBox cb : (ExerciseWeekTable)updateTableComponent) {
            if (cb.isSelected()) {
                toUpdate.add(checkBoxToExercise.get(cb));
            }
        }

        if (haveUnlockables && unlockCheckbox.isSelected()) {
            UnlockExercisesAction unlockAction = new UnlockExercisesAction();
            unlockAction.setSuccessListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doDownloadAndUpdate(toDownload, toUpdate);
                }
            });
            unlockAction.run();
        } else {
            doDownloadAndUpdate(toDownload, toUpdate);
        }

        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_downloadButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllButtonActionPerformed
        boolean select = !selectAllButtonIsDeselecting;
        for (int i = 0; i < ((ExerciseWeekTable)downloadTableComponent).getRowCount(); ++i) {
            if (select && isUnlockable(i) && !unlockCheckbox.isSelected()) {
                // Don't check grayed out unlockables
                continue;
            }
            ((ExerciseWeekTable)downloadTableComponent).setSelected(i, select);
        }
        for (int i = 0; i < ((ExerciseWeekTable)updateTableComponent).getRowCount(); ++i) {
            ((ExerciseWeekTable)updateTableComponent).setSelected(i, select);
        }
        updateSelectAllButtonState();
    }//GEN-LAST:event_selectAllButtonActionPerformed

    private void unlockCheckboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_unlockCheckboxItemStateChanged
        boolean active = (evt.getStateChange() == ItemEvent.SELECTED);
        for (JCheckBox cb : unlockableCheckboxes) {
            cb.setEnabled(active);
            cb.setSelected(active);
        }
        updateSelectAllButtonState();
    }//GEN-LAST:event_unlockCheckboxItemStateChanged

    private void downloadAdaptiveExButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadAdaptiveExButtonActionPerformed
        DownloadAdaptiveExerciseAction action = new DownloadAdaptiveExerciseAction();
        action.downloadAdaptiveExercise();
    }//GEN-LAST:event_downloadAdaptiveExButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JButton downloadAdaptiveExButton;
    private javax.swing.JButton downloadButton;
    private javax.swing.JScrollPane downloadTable;
    private javax.swing.JTable downloadTableComponent;
    private javax.swing.JLabel downloadableLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JCheckBox unlockCheckbox;
    private javax.swing.JTable updateTableComponent;
    private javax.swing.JLabel updateableLabel;
    // End of variables declaration//GEN-END:variables
}
