/**
 * 2108516
 * 2206145
 * 2006728
 */

package quizgame;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Main GUI for the quiz game.
 * Uses a CardLayout-based screen system and integrates with the Controller,
 * server, database, and game timer to provide a full multiplayer quiz experience.
 */
public class QuizAppPhase1 extends JFrame {

    // ---- backend controller ----
    // Central bridge between this GUI, the server, and the database
    private final Controller controller = new Controller();

    // ---- card names ----
    // Keys used by CardLayout to switch between different screens
    private static final String CARD_LOGIN         = "login";
    private static final String CARD_SETTINGS      = "settings";
    private static final String CARD_HOST_MENU     = "host_menu";
    private static final String CARD_ROOMS         = "rooms";
    private static final String CARD_WAITING       = "waitooming";
    private static final String CARD_QUIZ          = "quiz";
    private static final String CARD_ROUND_RESULTS = "round_results";
    private static final String CARD_LEADERBOARD   = "leaderboard";
    private static final String CARD_ADD_QUESTIONS = "add_questions";
    private boolean isLightMode = false;   // default dark mode

    // ---- theme types ----
    // Simple enum to represent current color theme
    private enum ThemeType { DARK, LIGHT }

    private ThemeType currentTheme = ThemeType.DARK;
    private Color BG_TOP, BG_BOTTOM, CARD_BG, TEXT_MAIN, BUTTON_PRIMARY, BUTTON_SECONDARY;
    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // ---- music ----
    // Background music toggle and audio clip reference
    private boolean musicEnabled = false;
    private Clip bgClip;

    // ---- global UI ----
    // Root container for all screens, controlled by CardLayout
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    // login
    private JTextField nicknameField;
    private JLabel loginErrorLabel;
    private boolean isHost = false;   // Flag to distinguish Host vs Player

    // rooms / waiting
    private final DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private JList<String> roomList;
    private JTextField roomNameField;
    private final DefaultListModel<String> waitingPlayersModel = new DefaultListModel<>();
    private JList<String> waitingPlayersList;
    private JButton waitingStartButton;
    private String currentRoomName = "";

    // quiz
    private JLabel questionLabel;
    private final JButton[] answerButtons = new JButton[4];
    private JLabel timerLabel;
    private JProgressBar timerBar;
    private JLabel scoreLabel;
    private List<Question> quizQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentScore = 0;
    private QuizTimer quizTimer;

    // round results
    private final DefaultListModel<String> roundResultsModel = new DefaultListModel<>();
    private JList<String> roundResultsList;
    private JLabel correctAnswerLabel;

    // leaderboard
    private final DefaultListModel<String> leaderboardModel = new DefaultListModel<>();
    private JList<String> leaderboardList;

    public QuizAppPhase1() {
        super("Multiplayer Quiz - CPIT305 Project");
        controller.setGuiReference(this);  // Link controller back to this GUI

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);       // Center window on screen

        // Apply base font to common Swing components
        UIManager.put("Label.font", BASE_FONT);
        UIManager.put("Button.font", BASE_FONT);
        UIManager.put("TextField.font", BASE_FONT);

        applyTheme(currentTheme);          // Initialize theme colors
        buildScreens();                    // Build all UI screens
        setContentPane(root);              // Attach CardLayout root
    }

    // ============================================================
    // THEME + MUSIC
    // ============================================================

    // Set color palette based on selected theme (dark/light)
    private void applyTheme(ThemeType theme) {
        currentTheme = theme;
        if (theme == ThemeType.DARK) {
            BG_TOP    = new Color(40, 0, 70);
            BG_BOTTOM = new Color(15, 15, 30);
            CARD_BG   = new Color(30, 30, 40);
            TEXT_MAIN = new Color(230, 230, 240);
            BUTTON_PRIMARY   = new Color(128, 0, 255);
            BUTTON_SECONDARY = new Color(70, 70, 70);
        } else { // LIGHT
            BG_TOP    = new Color(220, 220, 255);
            BG_BOTTOM = new Color(180, 200, 255);
            CARD_BG   = new Color(245, 245, 255);
            TEXT_MAIN = new Color(30, 30, 40);
            BUTTON_PRIMARY   = new Color(0, 120, 215);
            BUTTON_SECONDARY = new Color(180, 180, 180);
        }
    }

    // Start background music if enabled and clip is ready
    private void startMusicIfEnabled() {
        if (!musicEnabled) return;
        if (bgClip != null && bgClip.isRunning()) return;

        try {
            if (bgClip == null) {
                // Load WAV file as an audio resource from the /quizgame package
                URL url = getClass().getResource("/quizgame/background.wav");
                if (url == null) {
                    System.err.println("background.wav not found on classpath.");
                    return;
                }
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                bgClip = AudioSystem.getClip();
                bgClip.open(audioIn);
            }
            bgClip.loop(Clip.LOOP_CONTINUOUSLY);  // Loop sound indefinitely
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing music: " + e.getMessage());
        }
    }

    // Stop background music if it is currently playing
    private void stopMusic() {
        if (bgClip != null && bgClip.isRunning()) {
            bgClip.stop();
        }
    }

    // ============================================================
    // build all screens
    // ============================================================

    // Create and register all screens in the CardLayout container
    private void buildScreens() {
        root.removeAll();
        root.add(buildLoginCard(),        CARD_LOGIN);
        root.add(buildSettingsCard(),     CARD_SETTINGS);
        root.add(buildHostMenuCard(),     CARD_HOST_MENU);
        root.add(buildRoomsCard(),        CARD_ROOMS);
        root.add(buildWaitingCard(),      CARD_WAITING);
        root.add(buildQuizCard(),         CARD_QUIZ);
        root.add(buildRoundResultsCard(), CARD_ROUND_RESULTS);
        root.add(buildLeaderboardCard(),  CARD_LEADERBOARD);
        root.add(buildAddQuestionsCard(), CARD_ADD_QUESTIONS);
        root.revalidate();
        root.repaint();
    }

    // ============================================================
    // Login
    // ============================================================

    // Build login screen where user chooses nickname and role (host/player)
    private JPanel buildLoginCard() {
        nicknameField = createTextField("Player1");
        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setForeground(new Color(255, 120, 120));

        JButton hostBtn    = createPrimaryButton("Login as Host");
        JButton playerBtn  = createPrimaryButton("Login as Player");
        JButton settingsBtn= createSecondaryButton("Settings");

        hostBtn.addActionListener(e -> handleLogin(true));
        playerBtn.addActionListener(e -> handleLogin(false));
        settingsBtn.addActionListener(e -> cardLayout.show(root, CARD_SETTINGS));

        JPanel content = column(
                headerLabel("Login"),
                smallLabel("Enter a nickname (3–15 letters/numbers)."),
                wrapField(nicknameField),
                loginErrorLabel,
                row(hostBtn, playerBtn),
                row(Box.createHorizontalStrut(10), settingsBtn)
        );

        return gradientScreen(content);
    }

    // Process login and navigate to the proper menu based on host/player role
    private void handleLogin(boolean asHost) {
        String nick = nicknameField.getText().trim();
        if (!controller.handleLogin(nick)) {
            loginErrorLabel.setText("Invalid nickname. Use 3–15 letters/numbers.");
            return;
        }

        isHost = asHost;

        // ⭐ FIX: Rebuild all screens AFTER role is known (host or player)
        buildScreens();

        loginErrorLabel.setText(" ");

        if (waitingStartButton != null) {
            waitingStartButton.setEnabled(isHost);
        }

        setTitle((isHost ? "Host - " : "Player - ") + nick);

        if (isHost) {
            cardLayout.show(root, CARD_HOST_MENU);
        } else {
            cardLayout.show(root, CARD_ROOMS);
        }

        startMusicIfEnabled();
    }

    // ============================================================
    // Settings (theme + music)
    // ============================================================

    // Build settings screen (theme selection and music toggle)
    private JPanel buildSettingsCard() {
        JRadioButton darkBtn  = new JRadioButton("Dark (Purple)", currentTheme == ThemeType.DARK);
        JRadioButton lightBtn = new JRadioButton("Light", currentTheme == ThemeType.LIGHT);
        darkBtn.setOpaque(false);
        lightBtn.setOpaque(false);
        darkBtn.setForeground(TEXT_MAIN);
        lightBtn.setForeground(TEXT_MAIN);

        ButtonGroup group = new ButtonGroup();
        group.add(darkBtn);
        group.add(lightBtn);

        JCheckBox musicCheck = new JCheckBox("Enable background music", musicEnabled);
        musicCheck.setOpaque(false);
        musicCheck.setForeground(TEXT_MAIN);

        JButton applyBtn = createPrimaryButton("Apply");
        JButton backBtn  = createSecondaryButton("Back");

        applyBtn.addActionListener(e -> {
            // Apply new theme and music preference
            ThemeType newTheme = darkBtn.isSelected() ? ThemeType.DARK : ThemeType.LIGHT;
            musicEnabled = musicCheck.isSelected();

            stopMusic();
            applyTheme(newTheme);
            buildScreens();                 // rebuild UI with new colors
            cardLayout.show(root, CARD_LOGIN);
        });

        backBtn.addActionListener(e -> cardLayout.show(root, CARD_LOGIN));

        JPanel content = column(
                headerLabel("Settings"),
                label("Theme:"),
                row(darkBtn, lightBtn),
                Box.createVerticalStrut(10),
                label("Sound:"),
                musicCheck,
                Box.createVerticalStrut(10),
                row(applyBtn, backBtn)
        );
        return gradientScreen(content);
    }

    // ============================================================
    // Host menu
    // ============================================================

    // Build host main menu (room management, questions, and quiz start)
    private JPanel buildHostMenuCard() {
        JButton roomsBtn     = createPrimaryButton("Manage Rooms");
        JButton addQBtn      = createPrimaryButton("Add Temporary Questions");
        JButton startQuizBtn = createPrimaryButton("Start Quiz (from waiting room)");
        JButton logoutBtn    = createSecondaryButton("Logout");

        roomsBtn.addActionListener(e -> cardLayout.show(root, CARD_ROOMS));
        addQBtn.addActionListener(e -> cardLayout.show(root, CARD_ADD_QUESTIONS));
        logoutBtn.addActionListener(e -> {
            isHost = false;
            stopMusic();
            cardLayout.show(root, CARD_LOGIN);
        });
        startQuizBtn.addActionListener(this::handleHostStartQuizButton);

        JPanel content = column(
                headerLabel("Host Menu"),
                label("Host: " + (controller.getCurrentUsername() == null ? "" : controller.getCurrentUsername())),
                row(roomsBtn, addQBtn),
                row(startQuizBtn, logoutBtn)
        );
        return gradientScreen(content);
    }

    // Host-only action to start the quiz when enough players are in the room
    private void handleHostStartQuizButton(ActionEvent e) {
        if (!isHost) return;
        if (currentRoomName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Create or join a room first.", "No Room", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (waitingPlayersModel.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 players in the room.", "Not enough players",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.startQuizForRoom(currentRoomName); // notify others
        startQuizLocal();
    }

    // ============================================================
    // Rooms
    // ============================================================

    // Build room selection and creation screen
    private JPanel buildRoomsCard() {
        roomNameField = createTextField("Room1");
        roomList = new JList<>(roomListModel);
        styleList(roomList);

        JButton createBtn = createPrimaryButton("Create Room");
        JButton joinBtn   = createPrimaryButton("Join Selected Room");
        JButton backBtn   = createSecondaryButton("Back");

        // --- Event handlers ---
        createBtn.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Room name cannot be empty.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentRoomName = roomName;
            if (!roomListModel.contains(roomName)) {
                roomListModel.addElement(roomName);
            }
            controller.createRoom(roomName);
            enterWaitingRoom();
        });

        joinBtn.addActionListener(e -> {
            String selected = roomList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a room first.", "No Room",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentRoomName = selected;
            controller.joinRoom(selected);
            enterWaitingRoom();
        });

        backBtn.addActionListener(e -> {
            if (isHost) {
                cardLayout.show(root, CARD_HOST_MENU);
            } else {
                cardLayout.show(root, CARD_LOGIN);
            }
        });

        // -------- HOST-ONLY UI --------
        JPanel hostCreateSection = column(
                smallLabel("Create new room (host)"),
                wrapField(roomNameField),
                row(createBtn)
        );

        // -------- PLAYER UI --------
        JPanel playerJoinSection = row(joinBtn, backBtn);

        // Build final layout
        JPanel content = column(
                headerLabel("Rooms"),
                label("Available rooms on server"),
                new JScrollPane(roomList),

                // Show host or player UI
                isHost ? hostCreateSection : playerJoinSection,

                // All users get these:
                (!isHost ? row(backBtn) : row(joinBtn, backBtn))
        );

        return gradientScreen(content);
    }

    // Move user into the waiting room screen and add "you" to the list
    private void enterWaitingRoom() {
        waitingPlayersModel.clear();
        waitingPlayersModel.addElement(controller.getCurrentUsername() + " (you)");
        if (waitingStartButton != null) {
            waitingStartButton.setEnabled(isHost);
        }
        cardLayout.show(root, CARD_WAITING);
    }

    // Add new room to the list when notified by server
    public void onRoomCreated(String roomName) {
        if (!roomListModel.contains(roomName)) {
            roomListModel.addElement(roomName);
        }
    }

    // ============================================================
    // Waiting room
    // ============================================================

    // Build waiting room screen where players wait for the host to start
    private JPanel buildWaitingCard() {
        waitingPlayersList = new JList<>(waitingPlayersModel);
        styleList(waitingPlayersList);

        JButton backBtn = createSecondaryButton("Leave Room");
        waitingStartButton = createPrimaryButton("Start Quiz (Host)");
        waitingStartButton.setEnabled(isHost);

        backBtn.addActionListener(e -> {
            currentRoomName = "";
            if (isHost) {
                cardLayout.show(root, CARD_HOST_MENU);
            } else {
                cardLayout.show(root, CARD_ROOMS);
            }
        });

        waitingStartButton.addActionListener(this::handleHostStartQuizButton);

        JPanel content = column(
                headerLabel("Waiting Room"),
                label("Room: players joining live"),
                new JScrollPane(waitingPlayersList),
                smallLabel("Players will appear here as they join."),
                row(waitingStartButton, backBtn)
        );
        return gradientScreen(content);
    }

    // Called when server informs that a new player joined the current room
    public void onPlayerJoined(String room, String playerName) {
        if (!room.equals(currentRoomName)) return;
        if (!waitingPlayersModel.contains(playerName)) {
            waitingPlayersModel.addElement(playerName);
        }
    }

    // Called from Controller when server sends a QUIZ_START message
    public void onQuizStartFromServer() {
        if (!isHost) startQuizLocal();
    }

    // ============================================================
    // Quiz
    // ============================================================

    // Build quiz screen: question, answer buttons, timer, and score
    private JPanel buildQuizCard() {
        questionLabel = headerLabel("Question will appear here");
        questionLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel answersPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answersPanel.setOpaque(false);

        for (int i = 0; i < 4; i++) {
            JButton btn = createPrimaryButton("Option " + (i + 1));
            int idx = i;
            btn.addActionListener(e -> handleAnswerClicked(idx));
            answerButtons[i] = btn;
            answersPanel.add(btn);
        }
        if (isHost) {
            for (JButton btn : answerButtons) {
                btn.setEnabled(false);
                btn.setVisible(false);
            }
        }

        timerLabel = smallLabel("Time left: 0s");
        timerBar = new JProgressBar(0, 100);
        timerBar.setStringPainted(true);
        timerBar.setForeground(BUTTON_PRIMARY);
        timerBar.setBackground(CARD_BG.darker());

        scoreLabel = label("Score: 0");

        JPanel content = column(
                headerLabel("Quiz"),
                questionLabel,
                answersPanel,
                row(timerLabel, scoreLabel),
                timerBar
        );
        return gradientScreen(content);
    }

    // Start quiz locally by loading questions and showing the first one
    private void startQuizLocal() {
        quizQuestions = controller.getAllQuestionsForGame();
        if (quizQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No questions found (DB + host questions empty).",
                    "No Questions", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentQuestionIndex = 0;
        currentScore = 0;
        showQuestion();
        cardLayout.show(root, CARD_QUIZ);
    }

    // Render current question, reset timer, and enable answer buttons
    private void showQuestion() {
        if (quizTimer != null) quizTimer.stopTimer();

        if (currentQuestionIndex >= quizQuestions.size()) {
            endQuiz();
            return;
        }

        roundResultsModel.clear();
        Question q = quizQuestions.get(currentQuestionIndex);
        questionLabel.setText(q.getQuestionText());
        String[] opts = { q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD() };
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(opts[i]);
            answerButtons[i].setEnabled(true);
        }

        // ⭐ HOST CANNOT PLAY ⭐
        if (isHost) {
            for (JButton btn : answerButtons) {
                btn.setEnabled(false);   // disable answer buttons for host
            }
        }

        scoreLabel.setText("Score: " + currentScore);

        int time = Math.max(5, q.getTimeLimit());
        quizTimer = new QuizTimer(timerBar, timerLabel, time, () -> timeIsUpForQuestion(q));
        quizTimer.start();
    }

    // Handle case when time finishes before the player answers
    private void timeIsUpForQuestion(Question q) {

        // ⭐ Host should NOT send "No answer" or update score — just skip
        if (isHost) {
            showRoundResultsScreen(q);
            return;
        }

        for (JButton btn : answerButtons) btn.setEnabled(false);

        // Player: no answer chosen
        addOrUpdateRoundEntry(controller.getCurrentUsername(), "No answer", q.getCorrectAnswer(), false);
        controller.sendAnswerResult("No answer", q.getCorrectAnswer(), false);

        showRoundResultsScreen(q);
    }

    // Handle player clicking on one of the answer buttons
    private void handleAnswerClicked(int index) {
        if (isHost) return;
        if (currentQuestionIndex >= quizQuestions.size()) return;
        Question q = quizQuestions.get(currentQuestionIndex);
        String chosen = answerButtons[index].getText();
        boolean correct = chosen.equals(q.getCorrectAnswer());
        if (quizTimer != null) quizTimer.stopTimer();

        int gained = 0;
        if (correct) {
            int base = q.getPointsValue();
            int percent = timerBar.getValue();
            double factor = 0.3 + 0.7 * (percent / 100.0); // 30%–100%
            gained = (int) Math.round(base * factor);
            currentScore += gained;
        }

        scoreLabel.setText("Score: " + currentScore +
                (correct ? "  (+" + gained + ")" : "  (0)"));
        for (JButton btn : answerButtons) btn.setEnabled(false);

        // Notify server of new score and specific answer result
        controller.sendScoreUpdate(currentScore);
        controller.sendAnswerResult(chosen, q.getCorrectAnswer(), correct);
        addOrUpdateRoundEntry(controller.getCurrentUsername(), chosen, q.getCorrectAnswer(), correct);

        // Small delay before showing round results screen
        javax.swing.Timer t = new javax.swing.Timer(800, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            showRoundResultsScreen(q);
        });
        t.setRepeats(false);
        t.start();
    }

    // Show results for the current round and reveal the correct answer
    private void showRoundResultsScreen(Question q) {
        correctAnswerLabel.setText("Correct answer: " + q.getCorrectAnswer());
        cardLayout.show(root, CARD_ROUND_RESULTS);
    }

    // End quiz flow: save result, delete temp questions (host), show leaderboard
    private void endQuiz() {
        controller.finishGame(currentScore);

        // الهوست هو اللي أضاف الأسئلة المؤقتة
        if (isHost) {
            controller.deleteTemporaryQuestions();
        }

        updateLeaderboardWithLocalScore();
        cardLayout.show(root, CARD_LEADERBOARD);
    }

    // Initialize leaderboard for local player (and host)
    private void updateLeaderboardWithLocalScore() {
        leaderboardModel.clear();
        String name = controller.getCurrentUsername() == null ? "You" : controller.getCurrentUsername();
        leaderboardModel.addElement(name + " - " + currentScore + " pts");
    }

    // called from Controller when server broadcasts scores
    public void onScoreBroadcast(String playerName, int score) {
        String entry = playerName + " - " + score + " pts";
        if (!leaderboardModel.contains(entry)) {
            leaderboardModel.addElement(entry);
        }
    }

    // called from Controller when server broadcasts round results
    public void onRoundResult(String player, String chosen, String correct, boolean ok) {
        addOrUpdateRoundEntry(player, chosen, correct, ok);
    }

    // Handle NEXT_ROUND message received from server (player side)
    public void onNextRoundFromServer() {
        // الهوست هو اللي ضغط الزر أصلاً، فـ ما نكرر المنطق عنده
        if (isHost) return;

        // ننتقل للسؤال اللي بعده
        currentQuestionIndex++;
        if (currentQuestionIndex >= quizQuestions.size()) {
            endQuiz();
        } else {
            showQuestion();
            cardLayout.show(root, CARD_QUIZ);
        }
    }

    // Handle FINISH_QUIZ message from server (player side)
    public void onQuizFinishFromServer() {
        if (isHost) return;
        endQuiz();
    }

    // Add or update a single player's entry in the round results list
    private void addOrUpdateRoundEntry(String player, String chosen, String correct, boolean ok) {
        String prefix = player + ": ";
        String text = prefix + chosen;
        if (chosen == null || chosen.isEmpty()) text = prefix + "No answer";
        // simple list: just append every time (fine for small room)
        String decorated = (ok ? "✅ " : "❌ ") + text;
        if (!roundResultsModel.contains(decorated)) {
            roundResultsModel.addElement(decorated);
        }
    }

    // ============================================================
    // Round Results card
    // ============================================================

    // Build round results screen with summary and host controls
    private JPanel buildRoundResultsCard() {
        roundResultsList = new JList<>(roundResultsModel);
        styleList(roundResultsList);
        correctAnswerLabel = label("Correct answer: -");

        JButton nextBtn   = createPrimaryButton("Next Round");
        JButton finishBtn = createSecondaryButton("Finish Quiz");

        // Host triggers next round or end of quiz for everyone
        nextBtn.addActionListener(e -> {
            // هذا الزر للهوست فقط أصلاً، لكن نتحقق احتياطاً
            if (!isHost) return;

            currentQuestionIndex++;

            if (currentQuestionIndex >= quizQuestions.size()) {
                // خلصت الأسئلة: أبث انتهاء الكويز للجميع
                controller.sendFinishQuiz(currentRoomName);
                endQuiz();
            } else {
                // لا زال فيه أسئلة: أبث NEXT_ROUND للجميع
                controller.sendNextRound(currentRoomName);
                showQuestion();
                cardLayout.show(root, CARD_QUIZ);
            }
        });

        finishBtn.addActionListener(e -> {
            if (!isHost) return;
            controller.sendFinishQuiz(currentRoomName);
            endQuiz();
        });

        // ⭐ Host-only controls ⭐
        if (!isHost) {
            nextBtn.setEnabled(false);
            nextBtn.setVisible(false);

            finishBtn.setEnabled(false);
            finishBtn.setVisible(false);
        }

        JPanel content = column(
                headerLabel("Results & Leaderboard"),
                label("Round Results"),
                new JScrollPane(roundResultsList),
                correctAnswerLabel,
                label("Leaderboard"),
                new JScrollPane(getSharedLeaderboardListForResults()),
                row(nextBtn, finishBtn)
        );
        return gradientScreen(content);
    }

    // use same leaderboard list instance
    private JList<String> getSharedLeaderboardListForResults() {
        if (leaderboardList == null) {
            leaderboardList = new JList<>(leaderboardModel);
            styleList(leaderboardList);
        }
        return leaderboardList;
    }

    // ============================================================
    // Leaderboard
    // ============================================================

    // Build final leaderboard screen showing rankings and back navigation
    private JPanel buildLeaderboardCard() {
        leaderboardList = new JList<>(leaderboardModel);
        styleList(leaderboardList);

        JButton backBtn = createPrimaryButton("Back");
        backBtn.addActionListener(e -> {
            if (isHost) {
                cardLayout.show(root, CARD_HOST_MENU);
            } else {
                cardLayout.show(root, CARD_ROOMS);
            }
        });

        JPanel content = column(
                headerLabel("Final Leaderboard"),
                new JScrollPane(leaderboardList),
                backBtn
        );
        return gradientScreen(content);
    }

    // ============================================================
    // Add Temporary Questions
    // ============================================================

    // Build screen for host to add temporary questions dynamically
    private JPanel buildAddQuestionsCard() {

        // Fields
        JTextField qField       = createTextField("");
        JTextField aField       = createTextField("");
        JTextField bField       = createTextField("");
        JTextField cField       = createTextField("");
        JTextField dField       = createTextField("");
        JTextField correctField = createTextField("");
        JTextField timeField    = createTextField("15");
        JTextField pointsField  = createTextField("100");

        JLabel statusLabel      = smallLabel(" ");

        // Buttons
        JButton saveBtn = createPrimaryButton("Save Question");
        JButton backBtn = createSecondaryButton("Back");

        // Save action
        saveBtn.addActionListener(e -> {
            String qText = qField.getText().trim();
            String a = aField.getText().trim();
            String b = bField.getText().trim();
            String c = cField.getText().trim();
            String d = dField.getText().trim();
            String correct = correctField.getText().trim();
            int time, points;

            try {
                time = Integer.parseInt(timeField.getText().trim());
                points = Integer.parseInt(pointsField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Time and points must be numbers.",
                        "Invalid input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean ok = controller.handleAddQuestion(qText, a, b, c, d, correct, time, points);

            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "Fill all fields correctly.",
                        "Invalid input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            statusLabel.setText("Question saved! Total: " + controller.getHostQuestionCount());

            // Clear fields
            qField.setText("");
            aField.setText(""); bField.setText("");
            cField.setText(""); dField.setText("");
            correctField.setText("");

        });

        backBtn.addActionListener(e -> {
            cardLayout.show(root, CARD_HOST_MENU);
        });

        // Main form panel
        JPanel form = column(
                headerLabel("Add Temporary Questions"),
                smallLabel("These questions are NOT saved to the database."),
                label("Question:"),     wrapField(qField),
                label("Option A:"),     wrapField(aField),
                label("Option B:"),     wrapField(bField),
                label("Option C:"),     wrapField(cField),
                label("Option D:"),     wrapField(dField),
                label("Correct Answer:"), wrapField(correctField),
                label("Time limit (sec):"), wrapField(timeField),
                label("Points:"), wrapField(pointsField),
                statusLabel,
                row(saveBtn, backBtn)
        );

        // Allow scrolling if needed
        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll, BorderLayout.CENTER);

        return gradientScreen(wrapper);
    }

    // ============================================================
    // UI helpers
    // ============================================================

    // Create gradient background panel with centered card-style content
    private JPanel gradientScreen(JComponent content) {

        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth();
                int h = getHeight();

                Color c1, c2;

                // ⭐ Switch gradient based on theme ⭐
                if (currentTheme == ThemeType.LIGHT) {
                    c1 = new Color(245, 245, 255);   // top
                    c2 = new Color(225, 225, 240);   // bottom
                } else {
                    c1 = new Color(43, 0, 84);
                    c2 = new Color(15, 15, 45);
                }

                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
            }
        };

        background.setLayout(new GridBagLayout());

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        card.add(content, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(600, 400));
        card.setMaximumSize(new Dimension(700, 600));

        background.add(card);

        return background;
    }

    // Create large header label used as a title on screens
    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MAIN);
        l.setFont(BASE_FONT.deriveFont(Font.BOLD, 22f));
        return l;
    }

    // Create standard label with default text color
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MAIN);
        return l;
    }

    // Create smaller, secondary-style label
    private JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MAIN.darker());
        l.setFont(BASE_FONT.deriveFont(12f));
        return l;
    }

    // Create styled text field adapted to the current theme
    private JTextField createTextField(String initial) {
        JTextField tf = new JTextField(initial, 20);
        tf.setBackground(currentTheme == ThemeType.DARK
                ? new Color(25, 25, 35)
                : new Color(250, 250, 255));
        tf.setForeground(currentTheme == ThemeType.DARK
                ? new Color(240, 240, 240)
                : new Color(30, 30, 40));
        tf.setCaretColor(tf.getForeground());
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(90, 90, 90), 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    // Create primary action button with accent color
    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        styleButton(btn, BUTTON_PRIMARY);
        return btn;
    }

    // Create secondary action button with neutral color
    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        styleButton(btn, BUTTON_SECONDARY);
        return btn;
    }

    // Apply styling and hover behavior to a button
    private void styleButton(JButton btn, Color base) {
        btn.setForeground(Color.WHITE);
        btn.setBackground(base);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(base.darker(), 2, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setMargin(new Insets(8, 16, 8, 16));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {

                // 🌞 LIGHT MODE special hover for secondary buttons
                if (currentTheme == ThemeType.LIGHT && base.equals(BUTTON_SECONDARY)) {
                    btn.setBackground(new Color(180, 180, 180)); // soft gray hover
                }
                else {
                    // 🌙 DARK MODE & primary buttons use original behavior
                    btn.setBackground(base.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(base);
            }
        });
    }

    // Helper to align components in a horizontal row
    private JPanel row(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        for (Component c : comps) p.add(c);
        return p;
    }

    // Helper to stack components vertically with spacing
    private JPanel column(Component... comps) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component c : comps) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            p.add(c);
            p.add(Box.createVerticalStrut(8));
        }
        return p;
    }

    // Wrap a component (e.g., text field) in a borderless panel
    private JComponent wrapField(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    // Clear waiting players list model
    public void clearWaitingPlayers() {
        waitingPlayersModel.clear();
    }

    // Add a new player to the waiting list model
    public void addWaitingPlayer(String p) {
        waitingPlayersModel.addElement(p);
    }

    // Clear room list model
    public void clearRoomList() {
        roomListModel.clear();
    }

    // Add a new room name to the room list if not already present
    public void addRoomToList(String room) {
        if (!roomListModel.contains(room)) {
            roomListModel.addElement(room);
        }
    }

    // Apply styling to JList controls (waiting players, rooms, leaderboard, etc.)
    private void styleList(JList<?> list) {
        list.setBackground(currentTheme == ThemeType.DARK
                ? new Color(25, 25, 35)
                : new Color(240, 240, 255));
        list.setForeground(TEXT_MAIN);
        list.setSelectionBackground(new Color(90, 0, 160));
        list.setSelectionForeground(Color.WHITE);
        list.setBorder(new LineBorder(new Color(80, 80, 80), 1, true));
    }

    // ============================================================
    // main
    // ============================================================

    // Application entry point: launch GUI on the Event Dispatch Thread
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizAppPhase1 app = new QuizAppPhase1();
            app.setVisible(true);
        });
    }
}