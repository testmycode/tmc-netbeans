package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.coreimpl.ManualProgressObserver;
import fi.helsinki.cs.tmc.utilities.BrowserOpener;

import com.google.common.base.Optional;

import java.awt.event.ActionEvent;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

public class SubmissionResultWaitingDialog extends JDialog implements SubmissionProgressView {

    public static SubmissionResultWaitingDialog createAndShow(ManualProgressObserver observer) {
        final SubmissionResultWaitingDialog dialog = new SubmissionResultWaitingDialog(WindowManager.getDefault().getMainWindow(), observer);
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        return dialog;
    }

    private Cancellable task;

    /**
     * Creates new form SubmissionResultWaitingDialog
     */
    private SubmissionResultWaitingDialog(Frame parent, ManualProgressObserver observer) {
        super(parent, false);
        initComponents();
        queueLabel.setVisible(false);
        jProgressBar1.setIndeterminate(true);
        showSubmissionButton.setVisible(false);
        progressBarListening(observer);
    }

    @Override
    public void setPositionInQueueFromAnyThread(final int position) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (position > 1) {
                    queueLabel.setText("Place in queue: " + position);
                    queueLabel.setVisible(true);
                } else {
                    queueLabel.setVisible(false);
                }
            }
        });
    }

    public void setTask(Cancellable task) {
        this.task = task;
        cancelButton.setEnabled(task != null);
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    public void showSubmissionButton(URI showSubmissionUrl) {
        if (showSubmissionUrl == null) {
            return;
        }
        showSubmissionButton.setAction(new AbstractAction("View submission in browser") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    BrowserOpener.openUrl(showSubmissionUrl);
                } catch (URISyntaxException | IOException ex) {
                    ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                }
            }
        });
        showSubmissionButton.setVisible(true);
    }

    private void progressBarListening(ManualProgressObserver observer) {
        AtomicReference<Integer> progressBarMinimumAllowedValue = new AtomicReference<>(jProgressBar1.getValue());
        observer.addListener(message -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(multiLineTextIfNeeded(message.getMessage()));
                pack();
                Optional<Double> percentDone = message.getPercentDone();
                if (percentDone.isPresent()) {
                    jProgressBar1.setIndeterminate(false);
                    animateProgressbarTo(percentDone.get(), progressBarMinimumAllowedValue);
                }
            });
        });
    }

    private void animateProgressbarTo(Double percentDone, AtomicReference<Integer> minBound) {
        AtomicReference<Boolean> aborted = new AtomicReference<>(false);
        AtomicReference<Long> prevFrameFinish = new AtomicReference<>(System.currentTimeMillis());
        new Thread(() -> {
            int target = (int) (percentDone * 100);
            int current = jProgressBar1.getValue();
            int diff = target - current;
            long start = System.currentTimeMillis();
            int animationLength = 400;
            while (!aborted.get()) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        if (current >= target) {
                            aborted.set(true);
                            return;
                        }
                        long now = System.currentTimeMillis();
                        long timeDiff = now - start;
                        if (timeDiff > animationLength) {
                            if (target > minBound.get()) {
                                jProgressBar1.setValue(target);
                            }
                            aborted.set(true);
                            return;
                        }
                        double percentageToProgress = ((double) timeDiff) / animationLength;
                        int amountToProgress = (int) (diff * percentageToProgress);
                        int newCurrent = current + amountToProgress;
                        if (minBound.get() < newCurrent) {
                            minBound.set(newCurrent);
                            jProgressBar1.setValue(newCurrent);
                        }
                    });
                    long frameDiff = System.currentTimeMillis() - prevFrameFinish.get();
                    prevFrameFinish.set(System.currentTimeMillis());
                    int sleepTime = 16 - (int) frameDiff;
                    if (sleepTime >= 0) {
                        Thread.sleep(16 - frameDiff);
                    }
                } catch (InterruptedException ex) {
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }).start();
    }

    private String multiLineTextIfNeeded(String text) {
        if (text.length() > 50) {
            StringBuilder sb = new StringBuilder("<html>");
            String[] sentences = text.split("\\. | — ");
            for (String sentence : sentences) {
                StringBuilder lineBreakAdder = new StringBuilder(sentence);
                lineBreakAdder.append(".<br>");
                sb.append(lineBreakAdder.toString());
            }
            sb.replace(sb.length() - 5, sb.length(), "<html>");
            return sb.toString();
        }
        return text;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        waitingLabel = new javax.swing.JLabel();
        queueLabel = new javax.swing.JLabel();
        backgroundLabel = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        cancelButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        showSubmissionButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        waitingLabel.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.waitingLabel.text")); // NOI18N

        queueLabel.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.queueLabel.text")); // NOI18N

        backgroundLabel.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.backgroundLabel.text")); // NOI18N
        backgroundLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundLabelActionPerformed(evt);
            }
        });

        cancelButton.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.cancelButton.text")); // NOI18N
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        statusLabel.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.statusLabel.text")); // NOI18N

        showSubmissionButton.setText(org.openide.util.NbBundle.getMessage(SubmissionResultWaitingDialog.class, "SubmissionResultWaitingDialog.showSubmissionButton.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(queueLabel)
                            .addComponent(statusLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(showSubmissionButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(backgroundLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(waitingLabel)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(waitingLabel)
                .addGap(12, 12, 12)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queueLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(backgroundLabel)
                    .addComponent(showSubmissionButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backgroundLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundLabelActionPerformed
        close();
    }//GEN-LAST:event_backgroundLabelActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        task.cancel();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backgroundLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JLabel queueLabel;
    private javax.swing.JButton showSubmissionButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel waitingLabel;
    // End of variables declaration//GEN-END:variables
}
