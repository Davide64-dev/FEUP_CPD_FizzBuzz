package game;

import database.UserDatabase;
import user.User;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;


public class GameThread implements Runnable, Comparable<GameThread> {

    Socket clientSocket;
    public String username;
    public static UserDatabase database = new UserDatabase("src/database/users.csv");;
    public GameServer gameServer;
    private User user;

    private PrintWriter out;

    private BufferedReader in;

    GameThread(Socket clientSocket, GameServer gameServer){
        this.clientSocket = clientSocket;
        this.gameServer = gameServer;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean isAlive(){
        try {
            InputStream inputStream = this.getClientSocket().getInputStream();
            int data = inputStream.read();
            if (data == -1) {
                return false;
            }
        } catch (IOException ex) {}
        return true;
    }

    public User getUser(){
        return user;
    }

    public Socket getClientSocket(){
        return clientSocket;
    }

    @Override
    public int compareTo(GameThread other) {
        return this.user.compareTo(other.getUser());
    }

    String getUserInput(String query){
        out.println(query);
        try {
            return in.readLine();
        } catch (IOException e){
            return "";
        }
    }

    String getUserInput(){
        try {
            return in.readLine();
        } catch (IOException e){
            return "";
        }
    }

    public void outputToClient(String message){
        out.println(message);
    }

    public User loginOrRegister(){
        String option;
        do {
            option = getUserInput("Login (0) or Register (1)?");
        } while (!option.equals("0") && !option.equals("1"));

        if (option.equals("0")) return login();

        return register();
    }

    public User login(){
        String token = getUserInput();

        user = database.getUserByToken(token);

        if (user == null){
            System.out.println("The token is wrong or it is expired");

            outputToClient("invalid");

            // input request
            String username = getUserInput("Insert your username: ");
            String password = getUserInput("Insert your password: ");

            // user creation
            user = database.createUser(username, password);
            database.replaceUserToken(user.getToken(), user.getUsername());

            // Date logic
            String DateAsString = database.getTokenCreationDate(user.getUsername());
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime tokenCreationDate = LocalDateTime.parse(DateAsString, formatter);
            user.setDate(tokenCreationDate);

            // user info
            outputToClient(String.valueOf(user.getScore()));
            outputToClient(user.getToken());
        }

        else {
            outputToClient("valid");
            outputToClient(user.getUsername());
            outputToClient(String.valueOf(user.getScore()));
        }

        this.gameServer.addUser(this);
        outputToClient("valid");

        return user;
    }

    public User register(){
        // input request
        String username = getUserInput("Insert your username: ");
        String password, repeatPassword;

        do {
            password = getUserInput("Insert your password: ");
            repeatPassword = getUserInput("Repeat your password: ");
            if (!password.equals(repeatPassword)) outputToClient("invalid");
        } while (!password.equals(repeatPassword));

        outputToClient("valid");

        user = new User(username, 0);

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String formattedDate = currentDateTime.format(formatter);

        database.addUser(username, password, user.getToken(), formattedDate);

        outputToClient(String.valueOf(user.getScore()));
        outputToClient(user.getToken());

        if (!this.gameServer.addUser(this)) outputToClient("invalid");

        return user;
    }

    public static Comparator<GameThread> UserDateComparator = new Comparator<GameThread>() {
        @Override
        public int compare(GameThread thread1, GameThread thread2) {
            return thread1.getUser().getDate().compareTo(thread2.getUser().getDate());
        }
    };

    public void run() {
        System.out.println("Received a connection");

        User user = loginOrRegister();
        System.out.println(user);

        System.out.println("Connection closed");


    }
}
