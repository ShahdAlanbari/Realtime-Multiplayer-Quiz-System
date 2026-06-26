# Realtime Multiplayer Quiz System 🎮🌐

An interactive, real-time multiplayer quiz game system built using a robust Client-Server architecture in Java. The system demonstrates advanced backend core concepts including network socket programming, multithreaded concurrency control, database integration, and thread-safe graphical interfaces.

## 🚀 Key Features
* **Dynamic Lobby & Room System:** Allows hosts to dynamically create distinct game rooms, managing waiting lobbies and player connections efficiently.
* **Dual-Mode Question Bank:** Supports fetching permanent questions from a MySQL database or allowing hosts to inject offline, temporary questions through the UI.
* **Real-time Synchronization:** Implements live score tracking and active question countdown timers synchronized across all connected players.
* **Modern GUI:** Built with Java Swing featuring seamless dark/light theme toggling and responsive leaderboard screens.

## 🛠️ Technical Pillars & Architecture
* **Networking (`java.net`):** Leverages TCP Java Sockets for multi-directional, low-latency communication between the GameServer, Host, and Players.
* **Multithreading & Concurrency:** 
  * The server delegates every connected client to an isolated `ClientHandler` thread.
  * Utilizes `Collections.synchronizedMap` and block synchronization to avoid race conditions.
  * Dedicated background thread for `QuizTimer` preventing main UI thread freezing.
* **Database (JDBC):** Uses secured `PreparedStatement` routines to fetch and log question banks.
* **File I/O:** Generates localized text-based history logs (`.txt`) at the conclusion of every game session for auditing performance.
