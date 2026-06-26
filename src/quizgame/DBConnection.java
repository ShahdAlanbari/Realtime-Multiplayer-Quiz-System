package quizgame;
/**
 * 2108516
 * 2206145
 * 2006728
 */
import javax.swing.JOptionPane;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles creation and management of the MySQL database connection.
 * This class provides methods to read and insert quiz questions.
 */
public class DBConnection {

    private Connection con;

    public DBConnection() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish database connection
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/quizprojectdb",
                    "root",
                    "1128194758"
            );

        } catch (ClassNotFoundException e) {
            // Triggered if JDBC driver is missing
            JOptionPane.showMessageDialog(null,
                    "MySQL JDBC Driver not found.\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);

        } catch (SQLException e) {
            // Triggered if database connection fails
            JOptionPane.showMessageDialog(null,
                    "Unable to connect to database.\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConnected() {
        // Returns true only if the connection object is valid
        return con != null;
    }

    /**
     * Loads all questions belonging to the specified category.
     * Retrieves question text, options, correct answer, time limit, and points.
     */
    public List<Question> getQuestionsByCategory(String category) {
        List<Question> list = new ArrayList<>();
        if (con == null) return list; // If no connection, return empty list

        String sql = "SELECT question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, time_limit, points " +
                     "FROM questions WHERE category = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, category); // Bind category to query

            try (ResultSet rs = ps.executeQuery()) {

                // Convert each row in the ResultSet into a Question object
                while (rs.next()) {
                    list.add(new Question(
                            rs.getString("question_text"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d"),
                            rs.getString("correct_option"),
                            rs.getInt("time_limit"),
                            rs.getInt("points")
                    ));
                }
            }

        } catch (SQLException e) {
            // Error while reading data from DB
            System.err.println("DB error loading questions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Inserts a new question into the questions table.
     * Used when the host creates temporary questions to store in DB.
     */
    public boolean insertQuestion(String questionText,
                              String a, String b, String c, String d,
                              String correct, int time, int points,
                              String category) {

        if (con == null) return false;

        String sql = "INSERT INTO questions " +
            "(question_text, option_a, option_b, option_c, option_d, " +
            " correct_option, time_limit, points, category) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            // Bind all question data into the prepared statement
            ps.setString(1, questionText);
            ps.setString(2, a);
            ps.setString(3, b);
            ps.setString(4, c);
            ps.setString(5, d);
            ps.setString(6, correct);
            ps.setInt(7, time);
            ps.setInt(8, points);
            ps.setString(9, category);

                                                                                                                          
            ps.executeUpdate(); // Execute insertion
            return true;

        } catch (SQLException e) {
            // Triggered if SQL insertion fails
            System.err.println("Error inserting question: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all temporary questions (where is_temp = 1).
     * Typically called after a quiz session ends.
     */
    public void deleteTempQuestions() {
        if (con == null) return;

        String sql = "DELETE FROM questions WHERE is_temp = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate(); // Execute deletion
        } catch (SQLException e) {
            System.err.println("Error deleting temp questions: " + e.getMessage());
        }
    }

    /**
     * Safely closes the database connection.
     */
    public void close() {
        if (con != null) {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }
}

