package user;

import java.util.UUID;
import java.time.LocalDateTime;

public class User implements Comparable<User> {

    private String username;
    private int score;
    private String token;

    LocalDateTime date = LocalDateTime.now();

    public User(String username, int score){
        this.username = username;
        this.score = score;
        this.token = createRandomToken();
    }

    public void setDate(LocalDateTime newDate){
        date = newDate;
    }

    public LocalDateTime getDate(){
        return date;
    }

    public User(String username, int score, String token){
        this.username = username;
        this.score = score;
        this.token = token;
    }

    public void setToken(String newToken){
        this.token = newToken;
    }

    public static String createRandomToken(){
        return UUID.randomUUID().toString();
    }

    public String getUsername(){
        return username;
    }

    public int getScore(){
        return score;
    }

    public String getToken(){
        return token;
    }

    @Override
    public String toString() {
        return "\tusername='" + username +
                "\n\tscore=" + score +
                "\n\ttoken='" + token + '\'';
    }

    @Override
    public int compareTo(User other) {
        return this.date.compareTo(other.date);
    }

}

