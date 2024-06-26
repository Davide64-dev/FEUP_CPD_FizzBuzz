package game;

import user.User;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class GameClient {

    private Socket socket;

    private String token;

    private BufferedReader in;

    private PrintWriter out;

    private Scanner scanner;

    GameClient(String hostname, int port) throws UnknownHostException, IOException {
        readToken();
        try {
            this.socket = new Socket(hostname, port);
        } catch (UnknownHostException ex) {
            throw ex; // Re-throw UnknownHostException
        } catch (IOException ex) {
            throw ex; // Re-throw other IOExceptions
        }

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
    }

    public void closeConnection(){
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException ex){}
    }

    public void readToken(){
        try (BufferedReader br = new BufferedReader(new FileReader("src/user/token.txt"))) {
            this.token = br.readLine();
        } catch (IOException e) {
            System.out.println("Could not find the token");
            e.printStackTrace();
        }
    }

    public void writeToken(String token) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/user/token.txt"))) {
            bw.write(token);
        } catch (IOException e) {
            System.out.println("Could not write the token");
            e.printStackTrace();
        }
    }


    public void sendToken(){
        out.println(token);
    }

    public String readAndAnswer() throws IOException {
        try {
            String message = in.readLine();
            System.out.println(message);
            if (message.equals("Sorry, you lost!") || message.equals("Congratulations! You win!")){
                return "finish";
            }
            String answer = scanner.nextLine();
            out.println(answer);
            return answer;
        } catch (IOException ex){
            throw ex;
        }
    }

    public String read(){
        try {
            return in.readLine();
        }  catch (IOException ex){
            return "";
        }
    }

    private User loginOrRegister(){
        String option = "";
        try {
            while (!option.equals("0") && !option.equals("1")) {
                option = readAndAnswer();
            }
            if (option.equals("0")) return login();
            return register();

        } catch (IOException ex) {}
        return null;
    }

    private User register(){
        try {
            String username = readAndAnswer();
            String output = "invalid";
            while (!output.equals("valid")){
                readAndAnswer();
                readAndAnswer();
                output = read();
            }

            int score = Integer.parseInt(read());
            String token = read();

            User user = new User(username, score, token);
            writeToken(token);
            return user;


        } catch (IOException ex) {}

        return null;

    }

    private User login(){
        sendToken();

        String output = read();

        User user;

        try {

            if (!output.equals("valid")) {
                System.out.println("The token was invalid!");

                String username = readAndAnswer(); // username
                readAndAnswer(); // password

                int score = Integer.parseInt(read()); // score

                String token = read(); // token

                writeToken(token); // Replace the token to the new one

                user = new User(username, score, token);
            } else {
                System.out.println("The token was valid!");

                String username = read(); // username
                int score = Integer.parseInt(read()); // score
                user = new User(username, score, token);
            }

            if (read().equals("valid")){
                return user;
            }

            System.out.println("The user is already logged in in another machine!");
            return null;
        } catch (IOException e){
            return null;
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        if (args.length < 2) return;

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        try  {
            GameClient gameClient = new GameClient(hostname, port);


            User user = gameClient.loginOrRegister();
            System.out.println(user);

            if (user == null) return;

            while(true){
                String answer = gameClient.readAndAnswer();
                if (answer.equals("finish")) break;
            }

            System.out.println("Game finished!");


        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
