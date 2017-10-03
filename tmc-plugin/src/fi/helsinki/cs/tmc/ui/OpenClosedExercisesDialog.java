package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.CheckProjectCount;
import fi.helsinki.cs.tmc.actions.OpenClosedExercisesAction;
import fi.helsinki.cs.tmc.core.domain.Exercise;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.openide.windows.WindowManager;

public class OpenClosedExercisesDialog extends JDialog {

    public static void display(List<Exercise> closed) {
        OpenClosedExercisesDialog dialog = new OpenClosedExercisesDialog(closed);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
        
    private final List<Exercise> uncompleted;
    private final List<Exercise> completedOrExpired;
    private final HashMap<JCheckBox, Exercise> checkBoxToExercise;
    
    private boolean selectUncompletedButtonIsDeselecting;
    private boolean selectCompletedOrExpiredButtonIsDeselecting;

    private OpenClosedExercisesDialog(List<Exercise> closed) {
        super(WindowManager.getDefault().getMainWindow(), true);
        initComponents();

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        this.uncompleted = new ArrayList<>();
        this.completedOrExpired = new ArrayList<>();
        checkCompletion(closed);
        
        if (uncompleted.isEmpty()) {
            remove(uncompletedLabel);
            remove(uncompletedScrollPane);
            remove(selectUncompletedButton);
        }
        if (completedOrExpired.isEmpty()) {
            remove(completedOrExpiredLabel);
            remove(completedOrExpiredScrollPane);
            remove(selectCompletedOrExpiredButton);
        }
        if (closed.isEmpty()) {
            remove(openExercisesButton);
            titleLabel.setText("You don't have any closed projects!");
        }

        checkBoxToExercise = new HashMap<>();
        for (Exercise ex : uncompleted) {
            JCheckBox cb = new JCheckBox(ex.getName(), true);
            checkBoxToExercise.put(cb, ex);
            ((CheckBoxList)uncompletedList).addCheckbox(cb);
        }
        
        for (Exercise ex : completedOrExpired) {
            String text = ex.getName();
            if (ex.isCompleted()) {
                text += " (completed)";
            } else {
                text += " (expired at " + ex.getDeadline() + ")";
            }
            JCheckBox cb = new JCheckBox(text, false);
            checkBoxToExercise.put(cb, ex);
            ((CheckBoxList)completedOrExpiredList).addCheckbox(cb);
        }

        ((CheckBoxList)uncompletedList).addItemListener(selectUncompletedButtonStateListener);
        ((CheckBoxList)completedOrExpiredList).addItemListener(selectCompletedOrExpiredButtonStateListener);
        selectUncompletedButtonState();
        selectCompletedOrExpiredButtonState();
        
        pack();
    }
    
    private final ItemListener selectUncompletedButtonStateListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            selectUncompletedButtonState();
        }
    };
    
    private final ItemListener selectCompletedOrExpiredButtonStateListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            selectCompletedOrExpiredButtonState();
        }
    };
    
    private void selectUncompletedButtonState() {
        if (((CheckBoxList)uncompletedList).isAnySelected()) {
            selectUncompletedButtonIsDeselecting = true;
            selectUncompletedButton.setText("Unselect all");
        } else {
            selectUncompletedButtonIsDeselecting = false;
            selectUncompletedButton.setText("Select all");
        }
    }
    
    private void selectCompletedOrExpiredButtonState() {
        if (((CheckBoxList)completedOrExpiredList).isAnySelected()) {
            selectCompletedOrExpiredButtonIsDeselecting = true;
            selectCompletedOrExpiredButton.setText("Unselect all");
        } else {
            selectCompletedOrExpiredButtonIsDeselecting = false;
            selectCompletedOrExpiredButton.setText("Select all");
        }
    }
    
    private boolean doOpenExercises(List<Exercise> toOpen) {
        int alreadyOpenExercises = new CheckProjectCount().getProjectCount();
        boolean openExercises = true;
        if (alreadyOpenExercises + toOpen.size() > 50) {
            openExercises = showWarningIfOpeningTooManyExercises();
        }
        if (openExercises) {
            new OpenClosedExercisesAction().run(toOpen);
        }
        return openExercises;
    }
    
    private boolean showWarningIfOpeningTooManyExercises() {
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to open this many exercises? It may slow down your Netbeans!", "Opening too many exercises", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, new ImageIcon("infobubble.png"));
        return response == JOptionPane.YES_OPTION;
    }
    
    private void checkCompletion(List<Exercise> closed) {
        for (Exercise ex : closed) {
            if (!ex.isCompleted()) {
                this.uncompleted.add(ex);
            } else {
                this.completedOrExpired.add(ex);
            }
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

        uncompletedScrollPane = new javax.swing.JScrollPane();
        uncompletedList = new CheckBoxList();
        uncompletedLabel = new javax.swing.JLabel();
        completedOrExpiredLabel = new javax.swing.JLabel();
        completedOrExpiredScrollPane = new javax.swing.JScrollPane();
        completedOrExpiredList = new CheckBoxList();
        openExercisesButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        selectUncompletedButton = new javax.swing.JButton();
        titleLabel = new javax.swing.JLabel();
        selectCompletedOrExpiredButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.title")); // NOI18N

        uncompletedScrollPane.setViewportView(uncompletedList);

        uncompletedLabel.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.uncompletedLabel.text")); // NOI18N

        completedOrExpiredLabel.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.completedOrExpiredLabel.text")); // NOI18N

        completedOrExpiredScrollPane.setViewportView(completedOrExpiredList);

        openExercisesButton.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.openExercisesButton.text")); // NOI18N
        openExercisesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExercisesButtonActionPerformed(evt);
            }
        });

        closeButton.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        selectUncompletedButton.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.selectUncompletedButton.text")); // NOI18N
        selectUncompletedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectUncompletedButtonActionPerformed(evt);
            }
        });

        titleLabel.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.titleLabel.text")); // NOI18N

        selectCompletedOrExpiredButton.setText(org.openide.util.NbBundle.getMessage(OpenClosedExercisesDialog.class, "OpenClosedExercisesDialog.selectCompletedOrExpiredButton.text")); // NOI18N
        selectCompletedOrExpiredButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectCompletedOrExpiredButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(uncompletedScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(uncompletedLabel)
                            .addComponent(titleLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(completedOrExpiredScrollPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(completedOrExpiredLabel)
                        .addGap(283, 283, 283))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(openExercisesButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(closeButton))
                            .addComponent(selectCompletedOrExpiredButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(selectUncompletedButton, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(titleLabel)
                .addGap(18, 18, 18)
                .addComponent(uncompletedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uncompletedScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectUncompletedButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(completedOrExpiredLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(completedOrExpiredScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectCompletedOrExpiredButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton)
                    .addComponent(openExercisesButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void openExercisesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExercisesButtonActionPerformed
        final List<Exercise> toOpen = new ArrayList<>();
        for (JCheckBox cb : (CheckBoxList)uncompletedList) {
            if (cb.isSelected()) {
                toOpen.add(checkBoxToExercise.get(cb));
            }
        }
        
        for (JCheckBox cb : (CheckBoxList)completedOrExpiredList) {
            if (cb.isSelected()) {
                toOpen.add(checkBoxToExercise.get(cb));
            }
        }
        
        boolean closeThisWindow = true;
        
        if (!toOpen.isEmpty()) {
            closeThisWindow = doOpenExercises(toOpen);
        }
        
        if (closeThisWindow) {
            this.setVisible(false);
            this.dispose();
        }
    }//GEN-LAST:event_openExercisesButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void selectUncompletedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectUncompletedButtonActionPerformed
        boolean select = !selectUncompletedButtonIsDeselecting;
        
        for (int i = 0; i < ((CheckBoxList)uncompletedList).getElementCount(); ++i) {
            ((CheckBoxList)uncompletedList).setSelected(i, select);
        }

        selectUncompletedButtonState();
    }//GEN-LAST:event_selectUncompletedButtonActionPerformed

    private void selectCompletedOrExpiredButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectCompletedOrExpiredButtonActionPerformed
        boolean select = !selectCompletedOrExpiredButtonIsDeselecting;

        for (int i = 0; i < ((CheckBoxList)completedOrExpiredList).getElementCount(); ++i) {
            ((CheckBoxList)completedOrExpiredList).setSelected(i, select);
        }
        selectCompletedOrExpiredButtonState();
    }//GEN-LAST:event_selectCompletedOrExpiredButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel completedOrExpiredLabel;
    private javax.swing.JList completedOrExpiredList;
    private javax.swing.JScrollPane completedOrExpiredScrollPane;
    private javax.swing.JButton openExercisesButton;
    private javax.swing.JButton selectCompletedOrExpiredButton;
    private javax.swing.JButton selectUncompletedButton;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JLabel uncompletedLabel;
    private javax.swing.JList uncompletedList;
    private javax.swing.JScrollPane uncompletedScrollPane;
    // End of variables declaration//GEN-END:variables
}
