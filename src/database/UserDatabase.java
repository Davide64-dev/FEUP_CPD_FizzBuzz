package database;

import user.User;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UserDatabase {
    private String csvFilePath;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UserDatabase(String csvFilePath) {
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDir);

        this.csvFilePath = csvFilePath;
    }

    // Add a new user to the CSV file
    public void addUser(String username, String password, String token, String tokenCreationDate) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath, true))) {
            writer.println(username + "," + password + ",0," + token + "," + tokenCreationDate); // Assuming initial score is 0
            System.out.println("User added successfully");
        } catch (IOException e) {
            System.out.println("Error adding user: " + e.getMessage());
        }
    }

    public int getUserScore(String username) {
        try (Scanner scanner = new Scanner(new File(csvFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[0].equals(username)) {
                    return Integer.parseInt(parts[2]); // Score is at index 2
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("CSV file not found: " + e.getMessage());
        }
        return 0; // Default score if user not found or error occurs
    }

    public void updateUserScore(String username, int newScore) {
        try (Scanner scanner = new Scanner(new File(csvFilePath));
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath + ".tmp"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[0].equals(username)) {
                    writer.println(parts[0] + "," + parts[1] + "," + newScore + "," + parts[3] + "," + parts[4]);
                } else {
                    writer.println(line);
                }
            }
            // Rename the temporary file to replace the original
            new File(csvFilePath + ".tmp").renameTo(new File(csvFilePath));
            System.out.println("User score updated successfully");
        } catch (IOException e) {
            System.out.println("Error updating user score: " + e.getMessage());
        }
    }

    public boolean verifyPassword(String username, String password) {
        try (Scanner scanner = new Scanner(new File(csvFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[0].equals(username)) {
                    String storedPassword = parts[1]; // Password is at index 1
                    return storedPassword.equals(password);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("CSV file not found: " + e.getMessage());
        }
        return false; // Return false if user not found or error occurs
    }

    public User createUser(String username, String password) {
        if (verifyPassword(username, password)) {
            int score = getUserScore(username);
            return new User(username, score);
        } else {
            System.out.println("Invalid username or password");
            return null;
        }
    }

    // Method to get token creation date for a given username
    public String getTokenCreationDate(String username) {
        try (Scanner scanner = new Scanner(new File(csvFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[0].equals(username)) {
                    return parts[4]; // Token creation date is at index 4
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("CSV file not found: " + e.getMessage());
        }
        return null; // Return null if user not found or error occurs
    }

    public User getUserByToken(String token) {
        try (Scanner scanner = new Scanner(new File(csvFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[3].equals(token)) {
                    String username = parts[0];
                    int score = Integer.parseInt(parts[2]);
                    String tokenCreationDateStr = parts[4];

                    LocalDateTime tokenCreationDate = LocalDateTime.parse(tokenCreationDateStr, DATE_TIME_FORMATTER);
                    LocalDateTime now = LocalDateTime.now();

                    if (tokenCreationDate.isAfter(now.minusHours(1))) {
                        return new User(username, score);
                    } else {
                        System.out.println("Token is older than one hour");
                        return null;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("CSV file not found: " + e.getMessage());
        }
        return null; // Return null if user with token not found or error occurs
    }

    public void replaceUserToken(String newToken, String username) {
        String newTokenCreationDate = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        try (Scanner scanner = new Scanner(new File(csvFilePath));
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath + ".tmp"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 5 && parts[0].equals(username)) {
                    writer.println(parts[0] + "," + parts[1] + "," + parts[2] + "," + newToken + "," + newTokenCreationDate);
                } else {
                    writer.println(line);
                }
            }
            // Rename the temporary file to replace the original
            new File(csvFilePath + ".tmp").renameTo(new File(csvFilePath));
            System.out.println("User token replaced successfully");
        } catch (IOException e) {
            System.out.println("Error replacing user token: " + e.getMessage());
        }
    }
}
