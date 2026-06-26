package quizgame;
/**
 * 2108516
 * 2206145
 * 2006728
 */
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * Utility class responsible for saving the final game results
 * into a text file using basic Java I/O streams.
 */
public class GameLogger {

    // Function 4: Save Game Result (IO Stream requirement)
    public static void saveGameReport(String username, int finalScore) {

        // Appends game results into a persistent log file.
        // Each run adds a new entry without overwriting previous data.
        try (PrintWriter out = new PrintWriter(new FileWriter("Quiz_History_Report.txt", true))) {

            // Log structure written to the file
            out.println("--- Game Session Log ---");
            out.println("Date/Time: " + new Date());      // Timestamp of the session
            out.println("Player: " + username);           // Username of the player
            out.println("Final Score: " + finalScore + " points");  // Final score result
            out.println("------------------------------------");

        } catch (IOException e) {
            // If the file cannot be created or written to
            System.err.println("Error saving results file: " + e.getMessage());
        }
    }
}
