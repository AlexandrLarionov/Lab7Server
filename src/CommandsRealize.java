import javax.xml.parsers.ParserConfigurationException;
        import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
        import java.security.NoSuchAlgorithmException;
        import java.sql.SQLException;
        import java.util.HashMap;
        import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

// принимает от клиента объект Message, преобразовывает его в команду и выполняет её
public class CommandsRealize {
    String login = null;
    String password = null;
    private CopyOnWriteArrayList<Ticket> TicketCollection;
    private final boolean fromScript;
    private final Logger logger = Logger.getLogger("server.executor");
    private final DBManager dbManager;
    Map<String, String> users = new HashMap<>(); // список пользователей
    private final ExecutorService handlePool;
    private final Executor outPool;
    private Thread t;
    public CommandsRealize(DBManager dbManager, boolean fromScript, ExecutorService handlePool, Executor outPool) {
        try {
            this.TicketCollection = dbManager.readCollection();
        } catch (Exception e){
            System.out.println("EXCEPTION");
        }
        this.fromScript = fromScript;
        this.dbManager = dbManager;
        this.handlePool = handlePool;
        this.outPool = outPool;
    }

    public void execute(ObjectInputStream inputStream, DataOutputStream outputStream) throws ClassNotFoundException, ParserConfigurationException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-384");// для хэширования
        // принимаем сообщение
        boolean endOfStream = false;
        while (!endOfStream) {
            try {
                Object objMessage = inputStream.readObject();                                                // считали хоть что-то
                try {
                    ExtendMessage ext_message = (ExtendMessage) objMessage;                                                  // если обычное сообщение
                    Message message = ext_message.getMessage();
                    logger.info("message received");
                    if (message.isEnd) {
                        logger.info("Ctrl+D ??");
                        break;                                                                                // если кто-то умный нажал Ctrl+D
                    }
                    if (message.type == Commander.CommandType.exit && !message.metaFromScript)
                        endOfStream = true;                                                                   // заканчиваем принимать сообщения после команды exit не из скрипта
                    if (!validate(message.ticket) || message.ticket == null) throw new IOException();
                    Commander command = new Commander(outputStream, message.argument, message.ticket, dbManager, fromScript, login, password); // создаем Command и выполняем команду в одном из потоков
                    command.TypeChanger(message.type);
                    handlePool.execute(() -> {
                        try {
                            command.run(outPool);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e){
                            System.out.println("Unexpected problem");
                        }
                    });
                } catch (ClassCastException e) {                                                                // если сообщение авторизации
                    logger.info("authorization message got");
                    AuthorizateMessage message = (AuthorizateMessage) objMessage;
                    //message.password = new String(md.digest(message.password.getBytes()));// хэширование
                    message.password = encryptThisString(message.password, md);
                    DBManager dbManager = new DBManager(message.login, message.password);                       // смотрим таблицу юзеров по бд
                    dbManager.connect();
                    users = dbManager.readUserHashMap();
                    if (message.alreadyExist) {                                                                 // если авторизация то ищем в списке
                        if (users.containsKey(message.login) && users.get(message.login).equals(message.password)) {
                            login = message.login;                                                                // теперь юзер может все делать
                            logger.info("sign in successful");
                            outputStream.writeUTF("sign in successful");
                        } else {
                            logger.info("sign in not successful");
                            outputStream.writeUTF("sign in not successful");
                        }
                    } else {                                                                                    // если регистрация создаем новый
                        if (users.containsKey(message.login)) {
                            outputStream.writeUTF("There is already user with this login, try again");
                        } else{
                            users.put(message.login, message.password);
                            dbManager.addUser(message.login, message.password);                                 // добавляем юзера в бд
                            login = message.login;
                            outputStream.writeUTF("registration successful!");
                        }
                    }

                }
            } catch (IOException | SQLException e) { // если убили клиент, то ждём
                logger.info("can't receive message");
                break;
            }
        }

    }
    public static boolean validate(Ticket ticket) {
        try {
            Ticket ticket1 = new Ticket(ticket.getId(), ticket.getName(), ticket.getCoords(), ticket.getEvent(), ticket.getPrice(),  ticket.getType(),
                    ticket.getCreationDate(), ticket.getOwner());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String encryptThisString(String input, MessageDigest md)
    {
        try {
            // getInstance() method is called with algorithm SHA-384

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
