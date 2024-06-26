package game;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import database.UserDatabase;

public class Game implements Runnable {
    private List<GameThread> players;
    private List<Integer> scores;
    private static UserDatabase database = new UserDatabase("src/database/users.csv");
    private final Lock databaseLock = new ReentrantLock(); // Lock for database access
    private final Lock scoresLock = new ReentrantLock(); // Lock for scores access
    private GameServer gameServer;

    public Game(List<GameThread> players, GameServer gameServer) {
        // List of players participating
        this.players = new ArrayList<>(players);
        // List of scores for each player in this specific game
        this.scores = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            scores.add(0);
        }
        this.gameServer = gameServer;
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        System.out.println("Starting game with " + players.size() + " players");
        Random random = new Random();
        int round = 1;

        // Round starts
        while (true) {
            for (int i = 0; i < players.size(); i++) {
                GameThread player = players.get(i);
                int currentNumber = random.nextInt(round * 10) + 1;

                long startTime = System.currentTimeMillis();
                String answer = player.getUserInput("The number is... " + currentNumber);
                long endTime = System.currentTimeMillis();
                long timeTaken = (endTime - startTime) / 1000; // From miliseconds to seconds

                if (checkAnswer(answer, currentNumber)) {
                    scoresLock.lock();
                    try {
                        // Calculate time: 2 seconds give 10 points, every extra second is 1 less point
                        int score;
                        if (timeTaken <= 2) {
                            score = 10;
                        } else if (timeTaken <= 12) {
                            score = 12 - (int)timeTaken;
                        } else {
                            score = 0;
                        }
                        scores.set(i, scores.get(i) + score);
                    } finally {
                        scoresLock.unlock();
                    }
                }
            }
            round++;
            for (int i = 0; i < players.size(); i++) {
                if (scores.get(i) >= 15) {
                    GameThread player = players.get(i);
                    player.outputToClient("Congratulations! You win!");
                    updateRankings();
                    for (int j = 0; j < players.size(); j++) {
                        if (i == j) continue;
                        var loser = players.get(j);
                        loser.outputToClient("Sorry, you lost!");
                    }
                    gameServer.removeUsersFromGame(players);
                    return;
                }
            }
        }
    }

    private boolean checkAnswer(String answer, int num) {
        String correctAnswer = "";
        // If it's divisible by 3 say FIZZ, by 5 say BUZZ, by 7 say BANG
        if (num % 3 == 0) correctAnswer += "fizz";
        if (num % 5 == 0) correctAnswer += "buzz";
        if (num % 7 == 0) correctAnswer += "bang";
        // If it's not divisible by 3, 5, or 7, you have to say BOOM
        if (correctAnswer.isEmpty()) correctAnswer = "boom";
        // Putting answer in lowercase
        answer = answer.toLowerCase();
        // Removing whitespaces from answer
        answer = answer.replaceAll("\\s", "");

        return correctAnswer.equals(answer);
    }

    private void updateRankings() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparingInt(i -> -scores.get(i)));

        // Get the player with the highest score
        String bestUsername = players.get(indices.get(0)).username;
        int newScore;

        databaseLock.lock();
        try {
            // Update score for the best player
            newScore = database.getUserScore(bestUsername) + 75;
            database.updateUserScore(bestUsername, newScore);

            // Update scores for other players if in multiplayer mode
            if (players.size() > 1) {
                List<GameThread> winners = new ArrayList<>();
                List<GameThread> losers = new ArrayList<>();
                int middle = players.size() / 2;

                for (int i = 0; i < middle; i++) winners.add(players.get(indices.get(i)));
                for (int i = middle; i < players.size(); i++) losers.add(players.get(indices.get(i)));

                for (GameThread winner : winners) {
                    String username = winner.username;
                    if (!username.equals(bestUsername)) {
                        int winnerNewScore = database.getUserScore(username) + 25;
                        database.updateUserScore(username, winnerNewScore);
                    }
                }

                for (int i = 0; i < losers.size(); i++) {
                    String username = losers.get(i).username;
                    int penalty = (i == losers.size() - 1) ? 50 : 25;
                    int loserNewScore = database.getUserScore(username) - penalty;
                    database.updateUserScore(username, Math.max(0, loserNewScore)); // Ensure score is not negative
                }
            }
        } finally {
            databaseLock.unlock();
        }
    }

}
