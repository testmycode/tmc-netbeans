package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.DownloadExercisesAction;
import fi.helsinki.cs.tmc.actions.UnlockExercisesAction;
import fi.helsinki.cs.tmc.actions.UpdateExercisesAction;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Theme;

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

public class DownloadOrUpdateExercisesWithThemeDialog extends JDialog {

    public static void display(List<Exercise> unlockable, List<Exercise> downloadable, List<Exercise> updateable, List<Theme> themes) {
        DownloadOrUpdateExercisesWithThemeDialog dialog = new DownloadOrUpdateExercisesWithThemeDialog(unlockable, downloadable, updateable, themes);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    boolean haveUnlockables;

    private List<JCheckBox> unlockableCheckboxes;
    private HashMap<JCheckBox, Exercise> checkBoxToExercise;

    private boolean selectAllButtonIsDeselecting;

    private DownloadOrUpdateExercisesWithThemeDialog(List<Exercise> unlockable, List<Exercise> downloadable, List<Exercise> updateable, List<Theme> themes) {
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

        int downloadTableSize = downloadable.size();
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
            JCheckBox cb = new JCheckBox(text, true);
            Theme theme = themeForExercise(exercise, themes);
            checkBoxToExercise.put(cb, exercise);
            downloadRows[i][0] = cb;
            downloadRows[i][1] = theme.getName();
        }
        ((ExerciseThemeTable)downloadTableComponent).setRows(downloadRows);

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
            Theme theme = themeForExercise(exercise, themes);
            unlockableCheckboxes.add(cb);
            checkBoxToExercise.put(cb, exercise);
            ((ExerciseThemeTable)downloadTableComponent).setRow(cb, theme.getName());
        }

        for (int i = 0; i < updateTableSize; i++) {
            Exercise exercise = updateable.get(i);
            String text = exercise.getName();
            if (exercise.isCompleted()) {
                text += " (completed)";
            }
            JCheckBox cb = new JCheckBox(text, true);
            Theme theme = themeForExercise(exercise, themes);
            checkBoxToExercise.put(cb, exercise);
            updateRows[i][0] = cb;
            updateRows[i][1] = theme.getName();
        }
        ((ExerciseThemeTable)updateTableComponent).setRows(updateRows);
        ((ExerciseThemeTable)downloadTableComponent).addItemListener(updateSelectAllButtonStateListener);
        ((ExerciseThemeTable)updateTableComponent).addItemListener(updateSelectAllButtonStateListener);
        
        updateSelectAllButtonState();
        pack();
    }

        private Theme themeForExercise(Exercise exercise, List<Theme> themes) {
        if (themes == null) {
            return new Theme("no theme");
        }
        for (Theme theme : themes) {
            if (theme.shouldContain(exercise)) {
                return theme;
            }
        }
        return new Theme("no theme");
    }

    private ItemListener updateSelectAllButtonStateListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            updateSelectAllButtonState();
        }
    };

    private boolean isUnlockable(int index) {
        return unlockableCheckboxes.contains(((ExerciseThemeTable)downloadTableComponent).getElement(index));
    }

    private void updateSelectAllButtonState() {
        if (((ExerciseThemeTable)downloadTableComponent).isAnySelected() || ((ExerciseThemeTable)updateTableComponent).isAnySelected()) {
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
        downloadTableComponent = new ExerciseThemeTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        updateTableComponent = new ExerciseThemeTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.title")); // NOI18N

        downloadableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.downloadableLabel.text")); // NOI18N

        updateableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.updateableLabel.text")); // NOI18N

        downloadButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.downloadButton.text")); // NOI18N
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        closeButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        selectAllButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.selectAllButton.text")); // NOI18N
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllButtonActionPerformed(evt);
            }
        });

        unlockCheckbox.setSelected(true);
        unlockCheckbox.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesWithThemeDialog.class, "DownloadOrUpdateExercisesWithThemeDialog.unlockCheckbox.text")); // NOI18N
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(downloadTable)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(selectAllButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(downloadButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(closeButton))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(unlockCheckbox)
                            .addComponent(downloadableLabel)
                            .addComponent(updateableLabel))
                        .addGap(0, 294, Short.MAX_VALUE)))
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
                .addComponent(downloadTable, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateableLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton)
                    .addComponent(downloadButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
        final List<Exercise> toDownload = new ArrayList<Exercise>();
        for (JCheckBox cb : (ExerciseThemeTable)downloadTableComponent) {
            if (cb.isSelected()) {
                toDownload.add(checkBoxToExercise.get(cb));
            }
        }

        final List<Exercise> toUpdate = new ArrayList<Exercise>();
        for (JCheckBox cb : (ExerciseThemeTable)updateTableComponent) {
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
        for (int i = 0; i < ((ExerciseThemeTable)downloadTableComponent).getRowCount(); ++i) {
            if (select && isUnlockable(i) && !unlockCheckbox.isSelected()) {
                // Don't check grayed out unlockables
                continue;
            }
            ((ExerciseThemeTable)downloadTableComponent).setSelected(i, select);
        }
        for (int i = 0; i < ((ExerciseThemeTable)updateTableComponent).getRowCount(); ++i) {
            ((ExerciseThemeTable)updateTableComponent).setSelected(i, select);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
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
