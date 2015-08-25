package fi.helsinki.cs.tmc.utilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Invokes a given task in the EDT after a delay unless replaced by a newer task first.
 */
public class DelayedRunner {
    public static final int DEFAULT_DELAY = 1500;

    private javax.swing.Timer swingTimer;
    private int delay; // milliseconds

    public DelayedRunner() {
        this(DEFAULT_DELAY);
    }

    public DelayedRunner(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Schedules (possibly replaces) a task to run after the delay.
     */
    public void setTask(final Runnable task) {
        setTask(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        task.run();
                    }
                });
    }

    /**
     * Schedules (possibly replaces) a task to run after the delay.
     */
    public void setTask(ActionListener task) {
        cancel();

        if (task != null) {
            swingTimer = new javax.swing.Timer(delay, task);
            swingTimer.setRepeats(false);
            swingTimer.start();
        }
    }

    /**
     * Cancels the task.
     */
    public void cancel() {
        if (swingTimer != null) {
            swingTimer.stop();
        }
    }
}
