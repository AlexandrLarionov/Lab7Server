import java.io.Serializable;

public class AuthorizateMessage implements Serializable {
    String login;
    String password;
    boolean alreadyExist;
    public AuthorizateMessage(String login, String password, boolean alreadyExist){
        this.alreadyExist = alreadyExist;
        this.password = password;
        this.login = login;
    }
}