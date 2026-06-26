package quizgame;
/**
 * 2108516
 * 2206145
 * 2006728
 */
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * Controller class that connects the GUI with the server and DB.
 * Handles login, networking, question retrieval, and communication
 * between the interface and backend server.
 */
public class Controller {

    public DBConnection db;
    private Socket socket;
    private PrintWriter networkOut;
    private BufferedReader networkIn;
    private QuizAppPhase1 guiReference;
    private String currentUsername;
    private Thread listenerThread;

    // Temporary list used only when DB is unavailable (offline mode)
    private final List<Question> hostQuizQuestions = new ArrayList<>();

    public Controller() {
        db = new DBConnection();   // Initialize DB connection
    }

    public void sendNextRound(String roomName) {
        // Tell the server to broadcast next round to all players
        sendToServer("NEXT_ROUND:" + roomName);
    }

    public void sendFinishQuiz(String roomName) {
        // Notify server that host finished the quiz
        sendToServer("FINISH_QUIZ:" + roomName);
    }

    public void setGuiReference(QuizAppPhase1 gui) {
        // Attach GUI so server messages can update the interface
        this.guiReference = gui;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public int getHostQuestionCount() {
        // Number of temporary offline questions
        return hostQuizQuestions.size();
    }

    public List<Question> getAllQuestionsForGame() {
        // Returns all questions used in the quiz
        List<Question> all = new ArrayList<>();

        if (db != null && db.isConnected()) {
            // Online: load from database
            all.addAll(db.getQuestionsByCategory("default")); }
      //  } else {
            // Offline: use temporary in-memory questions
            all.addAll(hostQuizQuestions);
               return all;
        }

     
    

    // ---- Login ----
    public boolean handleLogin(String user) {
        // Validate username (3–15 alphanumeric characters)
        if (user == null) return false;
        String trimmed = user.trim();
        if (!trimmed.matches("[A-Za-z0-9]{3,15}")) return false;

        this.currentUsername = trimmed;

        // Connect to quiz server
        connectToNetwork();

        // Inform server about login
        sendToServer("LOGIN:" + currentUsername);
        return true;
    }

    // ---- Add temporary or DB question ----
    public boolean handleAddQuestion(String qText, String a, String b, String c,
                                    String d, String correct, int time, int points) {

        // Validate data before creating question
        if (qText == null || qText.length() < 5) return false;
        if (a == null || a.isEmpty() || b == null || b.isEmpty()
                || c == null || c.isEmpty() || d == null || d.isEmpty()
                || correct == null || correct.isEmpty()) return false;

        // Set default limits
        if (time <= 0) time = 15;
        if (points <= 0) points = 100;

        Question q = new Question(qText, a, b, c, d, correct, time, points);

        // Online mode → save directly to DB
        if (db != null && db.isConnected()) {
            db.insertQuestion(qText, a, b, c, d, correct, time, points, "default");
        } 
        else {
            // Offline mode → keep locally
           hostQuizQuestions.add(q);
       }

       return true;
    }

    // ---- Networking ----
    private void connectToNetwork() {
        try {
            // Connect to local quiz server
            socket = new Socket("localhost", 50001);

            // Prepare output/input streams
            networkOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            networkIn  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start background listener
            startListenerThread();
        } catch (IOException e) {
            System.err.println("Could not connect to server (offline mode): " + e.getMessage());
        }
    }

    private void startListenerThread() {
        // Background thread to constantly listen for server messages
        if (networkIn == null) return;

        listenerThread = new Thread(() -> {
            String line;
            try {
                while ((line = networkIn.readLine()) != null) {
                    // UI updates must run on the Swing UI thread
                    final String msg = line;
                    if (guiReference == null) continue;
                    SwingUtilities.invokeLater(() -> handleServerMessage(msg));
                }
            } catch (IOException e) {
                System.err.println("Server connection closed: " + e.getMessage());
            }
        }, "ServerListener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerMessage(String msg) {
        // Handle all incoming commands from server
        if (guiReference == null || msg == null) return;

        // Room creation broadcast
        if (msg.startsWith("ROOM_CREATED:")) {
            String roomName = msg.substring("ROOM_CREATED:".length());
            guiReference.onRoomCreated(roomName);
        }

        // Full list of rooms
        else if (msg.startsWith("ROOM_LIST:")) {
            String data = msg.substring("ROOM_LIST:".length());
            String[] rooms = data.split(",");

            SwingUtilities.invokeLater(() -> {
                guiReference.clearRoomList();
                for (String r : rooms) {
                    if (!r.trim().isEmpty()) {
                        guiReference.addRoomToList(r);
                    }
                }
            });
        }

        // Full list of players in waiting room
        else if (msg.startsWith("PLAYER_LIST:")) {
            String listStr = msg.substring("PLAYER_LIST:".length());
            String[] players = listStr.split(",");

            SwingUtilities.invokeLater(() -> {
                guiReference.clearWaitingPlayers();
                for (String p : players) {
                    if (!p.trim().isEmpty()) {
                        guiReference.addWaitingPlayer(p);
                    }
                }
            });
        }

        // Start quiz signal
        else if (msg.startsWith("QUIZ_START:")) {
            guiReference.onQuizStartFromServer();
        }

        // Score update broadcast
        else if (msg.startsWith("SCORE_BROADCAST:")) {
            String[] parts = msg.split(":", 3);
            if (parts.length >= 3) {
                String player = parts[1];
                int score;
                try {
                    score = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    score = 0;
                }
                guiReference.onScoreBroadcast(player, score);
            }
        }

        // Result for each player per round
        else if (msg.startsWith("ROUND_RESULT:")) {
            String[] parts = msg.split(":", 5);
            if (parts.length >= 5) {
                String player  = parts[1];
                String chosen  = parts[2];
                String correct = parts[3];
                boolean ok     = "1".equals(parts[4]);
                guiReference.onRoundResult(player, chosen, correct, ok);
            }
        }

        // Host triggered next round
        else if (msg.startsWith("NEXT_ROUND:")) {
            guiReference.onNextRoundFromServer();
        }

        // Host finished the quiz
        else if (msg.startsWith("FINISH_QUIZ:")) {
            guiReference.onQuizFinishFromServer();
        }
    }

    public void sendAnswerResult(String chosen, String correct, boolean isCorrect) {
        // Notify server of the answer selection
        String flag = isCorrect ? "1" : "0";
        sendToServer("ANSWER_RESULT:" + currentUsername + ":" + chosen + ":" + correct + ":" + flag);
    }

    private void sendToServer(String msg) {
        // Safe send to server
        if (networkOut != null && msg != null) {
            networkOut.println(msg);
        }
    }

    public void createRoom(String roomName) {
        // Request new room creation
        sendToServer("CREATE_ROOM:" + roomName);
    }

    public void joinRoom(String roomName) {
        // Join an existing room
        sendToServer("JOIN_ROOM:" + roomName + ":" + currentUsername);
    }

    public void startQuizForRoom(String roomName) {
        // Host triggers quiz start
        sendToServer("START_QUIZ:" + roomName);
    }

    public void sendScoreUpdate(int score) {
        // Push updated score to server
        sendToServer("SCORE_UPDATE:" + currentUsername + ":" + score);
    }

    // Finish game and save results locally
    public void finishGame(int finalScore) {
        sendToServer("SCORE_UPDATE:" + currentUsername + ":" + finalScore);
        GameLogger.saveGameReport(currentUsername, finalScore);
    }

    public void deleteTemporaryQuestions() {
        // Remove temporary questions from DB after quiz ends
        if (db != null && db.isConnected()) {
            db.deleteTempQuestions();
        }
    }
}
