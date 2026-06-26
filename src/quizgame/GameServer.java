package quizgame;
/**
 * 2108516
 * 2206145
 * 2006728
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Simple multi-client quiz server using text protocol.
 * Manages rooms, players, and broadcasts quiz events to all connected clients.
 */
public class GameServer {

    // Port on which the quiz server listens for incoming client connections
    private static final int PORT = 50001;

    // All connected clients (regardless of room)
    private static final Set<ClientHandler> handlers =
            Collections.synchronizedSet(new HashSet<>());

    // Mapping: roomName -> set of clients currently inside that room
    private static final Map<String, Set<ClientHandler>> rooms =
            Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        System.out.println("Quiz server starting on port " + PORT + "...");
        // Create a server socket and accept clients in an infinite loop
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();           // Wait for client
                ClientHandler handler = new ClientHandler(socket);
                handlers.add(handler);                           // Track all handlers
                handler.start();                                 // Handle in separate thread
                System.out.println("Client connected. Total clients: " + handlers.size());
            }
        }
    }

    // Broadcast a message to all connected clients (in all rooms)
    private static void broadcastToAll(String message) {
        synchronized (handlers) {
            for (ClientHandler h : handlers) {
                h.send(message);
            }
        }
    }

    // Broadcast a message only to clients inside a specific room
    private static void broadcastToRoom(String roomName, String message) {
        Set<ClientHandler> roomHandlers = rooms.get(roomName);
        if (roomHandlers == null) return;
        synchronized (roomHandlers) {
            for (ClientHandler h : roomHandlers) {
                h.send(message);
            }
        }
    }

    // Send a fresh list of players currently in a given room to everyone in that room
    private static void broadcastFullPlayerList(String roomName) {
        Set<ClientHandler> roomHandlers = rooms.get(roomName);
        if (roomHandlers == null) return;

        StringBuilder sb = new StringBuilder("PLAYER_LIST:");
        synchronized (roomHandlers) {
            for (ClientHandler h : roomHandlers) {
                sb.append(h.username).append(",");
            }
        }

        broadcastToRoom(roomName, sb.toString());
    }

    /**
     * Handles communication with a single client.
     * Each client runs on its own thread and processes incoming text commands.
     */
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username = "Unknown";
        private String currentRoom = null;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Set up I/O streams for this client
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;
                // Process messages from the client until connection is closed
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                System.out.println("Client I/O error: " + e.getMessage());
            } finally {
                // Ensure client cleanup even if an error occurs
                cleanup();
            }
        }

        // Handle a single message/command from the client
        private void handleMessage(String msg) {

            // User login command
            if (msg.startsWith("LOGIN:")) {
                username = msg.substring("LOGIN:".length());
                System.out.println("User logged in: " + username);

                // Send current room list to this newly logged-in client
                StringBuilder sb = new StringBuilder("ROOM_LIST:");
                synchronized (rooms) {
                    for (String r : rooms.keySet()) {
                        sb.append(r).append(",");
                    }
                }
                send(sb.toString());

            // Create a new room and add creator to it
            } else if (msg.startsWith("CREATE_ROOM:")) {
                String roomName = msg.substring("CREATE_ROOM:".length());
                rooms.putIfAbsent(roomName, Collections.synchronizedSet(new HashSet<>()));
                currentRoom = roomName;
                rooms.get(roomName).add(this);
                System.out.println(username + " created room " + roomName);

                // Notify all clients that a new room exists
                broadcastToAll("ROOM_CREATED:" + roomName);
                // Update player list in the room
                broadcastFullPlayerList(roomName);

            // Join an existing room
            } else if (msg.startsWith("JOIN_ROOM:")) {
                String[] parts = msg.split(":", 3);
                if (parts.length >= 2) {
                    String roomName = parts[1];
                    rooms.putIfAbsent(roomName, Collections.synchronizedSet(new HashSet<>()));
                    currentRoom = roomName;
                    rooms.get(roomName).add(this);
                    System.out.println(username + " joined room " + roomName);
                    // Refresh the player list inside this room
                    broadcastFullPlayerList(roomName);
                }

            // Start the quiz for a specific room (host action)
            } else if (msg.startsWith("START_QUIZ:")) {
                String roomName = msg.substring("START_QUIZ:".length());
                System.out.println("Starting quiz for room " + roomName);
                broadcastToRoom(roomName, "QUIZ_START:" + roomName);

            // Broadcast updated score to everyone in the same room
            } else if (msg.startsWith("SCORE_UPDATE:")) {
                String[] parts = msg.split(":", 3);
                if (parts.length >= 3) {
                    String player = parts[1];
                    String score = parts[2];
                    if (currentRoom != null) {
                        broadcastToRoom(currentRoom, "SCORE_BROADCAST:" + player + ":" + score);
                    }
                }

            // Result of a specific player's answer
            } else if (msg.startsWith("ANSWER_RESULT:")) {
                // format: ANSWER_RESULT:player:chosen:correct:isCorrect
                String[] parts = msg.split(":", 5);
                if (parts.length >= 5 && currentRoom != null) {
                    String player  = parts[1];
                    String chosen  = parts[2];
                    String correct = parts[3];
                    String flag    = parts[4]; // "1" or "0"
                    broadcastToRoom(currentRoom,
                            "ROUND_RESULT:" + player + ":" + chosen + ":" + correct + ":" + flag);
                }

            // Move all players in the room to the next round
            } else if (msg.startsWith("NEXT_ROUND:")) {
                String roomName = msg.substring("NEXT_ROUND:".length());
                broadcastToRoom(roomName, "NEXT_ROUND:" + roomName);

            // End the quiz for everyone in the room
            } else if (msg.startsWith("FINISH_QUIZ:")) {
                String roomName = msg.substring("FINISH_QUIZ:".length());
                broadcastToRoom(roomName, "FINISH_QUIZ:" + roomName);
            }
        }

        // Sends a full room list to a specific target client
        private static void broadcastFullRoomList(ClientHandler target) {
            StringBuilder sb = new StringBuilder("ROOM_LIST:");

            synchronized (rooms) {
                for (String room : rooms.keySet()) {
                    sb.append(room).append(",");
                }
            }

            target.send(sb.toString());
        }

        // Send a single text message to this client
        void send(String msg) {
            if (out != null) out.println(msg);
        }

        // Remove client from its room, from global set, and close its socket
        private void cleanup() {
            try {
                if (currentRoom != null) {
                    Set<ClientHandler> roomHandlers = rooms.get(currentRoom);
                    if (roomHandlers != null) {
                        synchronized (roomHandlers) {
                            roomHandlers.remove(this);
                        }
                    }
                }
                handlers.remove(this);
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Client disconnected. Total clients: " + handlers.size());
            } catch (IOException ignored) {}
        }
    }
}