package game;

import game.GameThread;
import user.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.abs;

public class GameServer {

    private ServerSocket ss;
    private final PriorityQueue<GameThread> waiting;
    private HashSet<String> players;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 5;
    private static volatile int INI_THRESHOLD = 5;
    private static double threshold = INI_THRESHOLD;
    private final Lock queueLock = new ReentrantLock();

    public GameServer(int port) {
        waiting = new PriorityQueue<>(GameThread.UserDateComparator);
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        players = new HashSet<>();

        new Thread(new GameStarter()).start();
        new Thread(new ThresholdUpdater()).start();
    }

    private class ThresholdUpdater implements Runnable {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000); // Sleep for one second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                threshold *= 1.1; // Increment threshold by ten percent
                System.out.println("Threshold updated to: " + threshold); // For debugging purposes
            }
        }
    }

    private class DisconnectionsUpdater implements Runnable{
        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000); // Sleep for one second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queueLock.lock();

                List<GameThread> gameList = new ArrayList<>();

                while (!waiting.isEmpty()){
                    var current = waiting.poll();
                    if (current.getClientSocket().isClosed() || current.getClientSocket().isInputShutdown() || current.getClientSocket().isOutputShutdown()) {
                        System.out.println("Closed!");
                        players.remove(current.getUser().getUsername());
                        continue;
                    }
                    System.out.println("Still Open!");
                    gameList.add(current);
                }

                waiting.addAll(gameList);

                queueLock.unlock();
            }
        }
    }

    private class GameStarter implements Runnable {
        public void run() {
            while (true) {
                synchronized (waiting) {
                    try {
                        while (waiting.size() < MIN_PLAYERS) {
                            System.out.println(waiting.size());
                            waiting.wait(); // Wait for enough players
                        }
                        // Start a new game with the required number of players
                        startGame();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void startGame() {
            queueLock.lock();
            try {
                List<GameThread> gameList = new ArrayList<>();
                List<GameThread> selectedPlayers = new ArrayList<>();

                int numPlayers = 0;
                int minPunctuation = Integer.MAX_VALUE;
                int maxPunctuation = Integer.MIN_VALUE;

                while (numPlayers < MAX_PLAYERS && !waiting.isEmpty()) {
                    GameThread player = waiting.poll();
                    if (player == null) {
                        System.out.println("User closed connection");
                        players.remove(player.getUser().getUsername());
                        continue;
                    }

                    int playerScore = player.getUser().getScore();
                    if (numPlayers == 0 || (abs(playerScore - minPunctuation) <= threshold && abs(playerScore - maxPunctuation) <= threshold)) {
                        selectedPlayers.add(player);
                        if (playerScore < minPunctuation) minPunctuation = playerScore;
                        if (playerScore > maxPunctuation) maxPunctuation = playerScore;
                        numPlayers++;
                    } else {
                        gameList.add(player);
                    }
                }

                if (numPlayers < MIN_PLAYERS) {
                    waiting.addAll(selectedPlayers);
                    waiting.addAll(gameList);
                    return;
                }

                waiting.addAll(gameList);
                threshold = INI_THRESHOLD;
                System.out.println("Starting game with players: " + selectedPlayers);

                new Thread(() -> {
                    Game game = new Game(selectedPlayers, GameServer.this);
                    game.start();
                }).start();
            } finally {
                queueLock.unlock();
            }
        }
    }

    public void run() {
        try {
            while (true) {
                Socket cs = ss.accept();
                GameThread gameThread = new GameThread(cs, this);
                Thread.startVirtualThread(gameThread); // Use virtual thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ss != null && !ss.isClosed()) {
                    ss.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean addUser(GameThread thread) {
        queueLock.lock();
        try {
            synchronized (waiting) {
                if (players.contains(thread.getUser().getUsername())) {
                    return false; // User already in the game
                }
                System.out.println("New user!");
                players.add(thread.getUser().getUsername());
                waiting.add(thread);
                waiting.notify(); // Notify the GameStarter thread
                System.out.println(waiting.size());
            }
            return true;
        } finally {
            queueLock.unlock();
        }
    }

    public void removeUsersFromGame(List<GameThread> gamePlayers) {
        queueLock.lock();
        try {
            for (GameThread player : gamePlayers) {
                players.remove(player.getUser().getUsername());
            }
        } finally {
            queueLock.unlock();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);

        GameServer server = new GameServer(port);

        server.run();
    }
}
