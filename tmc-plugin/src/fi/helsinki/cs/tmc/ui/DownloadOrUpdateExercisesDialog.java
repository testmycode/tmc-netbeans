package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.DownloadExercisesAction;
import fi.helsinki.cs.tmc.actions.UpdateExercisesAction;
import fi.helsinki.cs.tmc.data.Exercise;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import org.openide.windows.WindowManager;

public class DownloadOrUpdateExercisesDialog extends JDialog {

    public static void display(List<Exercise> downloadable, List<Exercise> updateable) {
        DownloadOrUpdateExercisesDialog dialog = new DownloadOrUpdateExercisesDialog(downloadable, updateable);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
    
    private List<Exercise> downloadable;
    private List<Exercise> updateable;
    
    private boolean selectAllButtonIsDeselecting;

    private DownloadOrUpdateExercisesDialog(List<Exercise> downloadable, List<Exercise> updateable) {
        super(WindowManager.getDefault().getMainWindow(), true);
        initComponents();

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.downloadable = downloadable;
        this.updateable = updateable;

        if (downloadable.isEmpty()) {
            remove(downloadableLabel);
            remove(downloadableScrollPane);
            setTitle("Update exercises");
            downloadButton.setText("Update");
        }
        if (updateable.isEmpty()) {
            remove(updateableLabel);
            remove(updateableScrollPane);
            setTitle("Download exercises");
            downloadButton.setText("Download");
        }

        for (Exercise ex : downloadable) {
            ((CheckBoxList)downloadableList).addCheckbox(new JCheckBox(ex.getName(), true));
        }

        for (Exercise ex : updateable) {
            ((CheckBoxList)updateableList).addCheckbox(new JCheckBox(ex.getName(), true));
        }

        ((CheckBoxList)downloadableList).addItemListener(updateSelectAllButtonStateListener);
        ((CheckBoxList)updateableList).addItemListener(updateSelectAllButtonStateListener);
        updateSelectAllButtonState();
        
        pack();
    }
    
    private ItemListener updateSelectAllButtonStateListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            updateSelectAllButtonState();
        }
    };
    
    private void updateSelectAllButtonState() {
        if (((CheckBoxList)downloadableList).isAnySelected() || ((CheckBoxList)updateableList).isAnySelected()) {
            selectAllButtonIsDeselecting = true;
            selectAllButton.setText("Unselect all");
        } else {
            selectAllButtonIsDeselecting = false;
            selectAllButton.setText("Select all");
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        downloadableScrollPane = new javax.swing.JScrollPane();
        downloadableList = new CheckBoxList();
        downloadableLabel = new javax.swing.JLabel();
        updateableLabel = new javax.swing.JLabel();
        updateableScrollPane = new javax.swing.JScrollPane();
        updateableList = new CheckBoxList();
        downloadButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        selectAllButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.title")); // NOI18N

        downloadableScrollPane.setViewportView(downloadableList);

        downloadableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.downloadableLabel.text")); // NOI18N

        updateableLabel.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.updateableLabel.text")); // NOI18N

        updateableScrollPane.setViewportView(updateableList);

        downloadButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.downloadButton.text")); // NOI18N
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        closeButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        selectAllButton.setText(org.openide.util.NbBundle.getMessage(DownloadOrUpdateExercisesDialog.class, "DownloadOrUpdateExercisesDialog.selectAllButton.text")); // NOI18N
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(updateableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                    .addComponent(downloadableLabel)
                    .addComponent(downloadableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                    .addComponent(updateableLabel)
                    .addComponent(selectAllButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(downloadButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(downloadableLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(downloadableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(updateableLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
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
        List<Exercise> toDownload = new ArrayList<Exercise>();
        for (int i = 0; i < downloadable.size(); ++i) {
            if (((CheckBoxList)downloadableList).isSelected(i)) {
                toDownload.add(downloadable.get(i));
            }
        }
        List<Exercise> toUpdate = new ArrayList<Exercise>();
        for (int i = 0; i < updateable.size(); ++i) {
            if (((CheckBoxList)updateableList).isSelected(i)) {
                toUpdate.add(updateable.get(i));
            }
        }

        new DownloadExercisesAction(toDownload).run();
        new UpdateExercisesAction(toUpdate).run();

        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_downloadButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllButtonActionPerformed
        boolean select = !selectAllButtonIsDeselecting;
        for (int i = 0; i < downloadable.size(); ++i) {
            ((CheckBoxList)downloadableList).setSelected(i, select);
        }
        for (int i = 0; i < updateable.size(); ++i) {
            ((CheckBoxList)updateableList).setSelected(i, select);
        }
        updateSelectAllButtonState();
    }//GEN-LAST:event_selectAllButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JButton downloadButton;
    private javax.swing.JLabel downloadableLabel;
    private javax.swing.JList downloadableList;
    private javax.swing.JScrollPane downloadableScrollPane;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JLabel updateableLabel;
    private javax.swing.JList updateableList;
    private javax.swing.JScrollPane updateableScrollPane;
    // End of variables declaration//GEN-END:variables
}