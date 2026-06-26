/**
 * 2108516
 * 2206145
 * 2006728
 */
package quizgame;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Countdown timer running in its own thread.
 * Updates a progress bar and label every second and triggers
 * a callback when the time is over.
 */
public class QuizTimer extends Thread {

    // Progress bar that visually shows remaining time as a percentage
    private final JProgressBar bar;

    // Label that displays remaining time in seconds as text
    private final JLabel label;

    // Total countdown duration in seconds
    private final int totalSeconds;

    // Action to execute when the timer finishes (e.g., auto-submit question)
    private final Runnable onFinishAction;

    // Flag used to safely stop the timer loop from outside the thread
    private volatile boolean running = true;

    /**
     * Constructs a new QuizTimer.
     *
     * @param bar            progress bar to be updated (can be null)
     * @param label          label to show remaining seconds (can be null)
     * @param totalSeconds   total time in seconds to count down from
     * @param onFinishAction callback to run when time reaches zero
     */
    public QuizTimer(JProgressBar bar, JLabel label, int totalSeconds, Runnable onFinishAction) {
        this.bar = bar;
        this.label = label;
        this.totalSeconds = totalSeconds;
        this.onFinishAction = onFinishAction;
    }

    /**
     * Stops the countdown loop and interrupts the sleeping thread.
     * This is called when the user answers before time is over.
     */
    public void stopTimer() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        int remaining = totalSeconds;

        // Main countdown loop (until stopped or reaches zero)
        while (running && remaining >= 0) {
            final int current = remaining;

            // Update Swing components on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                if (bar != null) {
                    int percent = (int) ((current / (double) totalSeconds) * 100);
                    bar.setValue(Math.max(0, percent));
                }
                if (label != null) {
                    label.setText("Time left: " + current + "s");
                }
            });

            if (remaining == 0) break;   // exit loop when time is up

            try {
                Thread.sleep(1000);       // wait 1 second between updates
            } catch (InterruptedException e) {
                // Thread was interrupted (e.g., stopTimer called), exit loop
                break;
            }
            remaining--;
        }

        // Only call finish action if timer was not cancelled
        if (running && onFinishAction != null) {
            SwingUtilities.invokeLater(onFinishAction);
        }
    }
}