//import com.sun.xml.internal.bind.v2.TODO;
//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;

// считывает команды из консоли и принимает ответы с сервера, выводит их в консоль
public class CommandReader {
    static String login = null;
    String password = null;
    InetSocketAddress address;
    SocketChannel channel;
    ByteArrayOutputStream byteArrayOutputStream;
    ObjectOutputStream objectOutputStream;
    boolean afterConnecting = false;
    public CommandReader(InetSocketAddress address) {
        this.address = address;
        connect();
    }
    // подключение к серверу
    void connect() {
        while (true) {
            try {
                channel = SocketChannel.open(address);
                byteArrayOutputStream = new ByteArrayOutputStream();
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                send(byteArrayOutputStream.toByteArray());
                byteArrayOutputStream.reset();
                return;
            } catch (IOException e) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                System.out.println("reconnecting");
            }
        }
    }
    // собственно загрузка байт в канал
    void send(byte[] message) throws IOException {
        int r = channel.write(ByteBuffer.wrap(message));
        if (r != message.length) {
            throw new IOException();
        }
    }
    // функция, которая отправляет message и получает ответ
    String getResponse(Object message) {
        while (true) {
            try {
                byteArrayOutputStream.reset();
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
                send(byteArrayOutputStream.toByteArray());

                ByteBuffer shortBuffer = ByteBuffer.allocate(2);
                int r = channel.read(shortBuffer);
                if (r == -1) {
                    throw new IOException();
                }
                shortBuffer.flip();
                short len = shortBuffer.getShort();
                ByteBuffer buffer = ByteBuffer.allocate(len);
                r = channel.read(buffer);
                if (r == -1) {
                    throw new IOException();
                }
                buffer.flip();
                return StandardCharsets.UTF_8.decode(buffer).toString();
            } catch (IOException e) {
                connect();
            }
        }
    }
    String tryRead() {
        try {
            ByteBuffer shortBuffer = ByteBuffer.allocate(2);
            int r = channel.read(shortBuffer);
            if (r == -1) {
                throw new IOException();
            }
            shortBuffer.flip();
            short len = shortBuffer.getShort();
            ByteBuffer buffer = ByteBuffer.allocate(len);
            r = channel.read(buffer);
            if (r == -1) {
                throw new IOException();
            }
            buffer.flip();
            return StandardCharsets.UTF_8.decode(buffer).toString();
        } catch (IOException e) { return null;}
    }
    // основная функция взаимодействия (считывание команд и тд)
    public boolean read(Scanner scanner, boolean fromScript) throws IOException {
        boolean exitStatus = false;
        Ticket ticket = new Ticket();
        boolean wasEnter = false;                                 // для проверки нажатия на клавишу Enter
        // here authorization
        if (!fromScript){
            String authMessage = "";
            while (!(authMessage.equals("sign in successful") || authMessage.equals("registration successful!"))){     // пока не авторизуется спрашиваем
                authMessage = authorization(scanner);
                System.out.println(authMessage);
            }
        }
        while (!exitStatus) {
            afterConnecting = false;
            String[] text = null;
            Commander.CommandType type = null;
            if (!fromScript && !wasEnter) System.out.println("Enter command");
            wasEnter = false;
            if (scanner.hasNext()) {
                String textline = scanner.nextLine();
                if (textline.trim().isEmpty()) {wasEnter = true; continue;}
                text = textline.replaceAll("^\\s+", "").split(" ", 2);
            } else {
                objectOutputStream.writeObject(new ExtendMessage(new Message(true), login, password));
                objectOutputStream.flush();
                System.exit(0);
            }
            String word = text[0];
            String argument;
            try {
                argument = text[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                argument = null;
            }
            boolean normalCommand = true;
            switch (word) {
                case ("update"):
                    if (argument == null || !ArgPars(argument)) {
                        System.out.println("Invalid argument");
                        normalCommand = false;
                    } else {
                        type = Commander.CommandType.update;
                        ticket = inputTicket();
                    }
                case ("help"):
                    type = Commander.CommandType.help;
                    break;
                case ("info"):
                    type = Commander.CommandType.info;
                    break;
                case ("show"):
                    type = Commander.CommandType.show;
                    break;
                case ("clear"):
                    type = Commander.CommandType.clear;
                    break;
                case ("exit"):
                    exitStatus = true;
                    type = Commander.CommandType.exit;
                    break;
                case ("count_by_price"):
                    type = Commander.CommandType.count_by_price;
                    break;
                case ("add"):
                    type = Commander.CommandType.add;
                    ticket = inputTicket();
                    break;
                case ("insert_at"):
                    type = Commander.CommandType.insert_at;
                    ticket = inputTicket();
                    break;
                case ("add_if_min"):
                    type = Commander.CommandType.add_if_min;
                    ticket = inputTicket();
                    break;
                case ("add_if_max"):
                    type = Commander.CommandType.add_if_max;
                    ticket = inputTicket();
                    break;
                case ("unique_type"):
                    type = Commander.CommandType.unique_type;
                    break;

                case ("remove_by_id"):
                    type = Commander.CommandType.remove_by_id;
                    break;
                case ("execute_script"):
                    type = Commander.CommandType.execute_script;
                    if (fromScript) {
                        System.out.println("Danger of recursion, skipping command");
                    }
                    else {
                        execute_script(argument);
                    }
                    break;
                case ("shuffle"):
                    type = Commander.CommandType.shuffle;
                    break;
                case ("average_of_price"):
                    type = Commander.CommandType.average_price;
                    break;
                default:
                    System.out.println("Invalid command. Try 'help' to see the list of commands");
                    normalCommand = false;
                    break;
            }
            try {            // если нормальная команда отправляем на сервер
                if (normalCommand) {
                    ExtendMessage message = new ExtendMessage(new Message(ticket, type, argument, fromScript), login, password);
                    if (!(type == Commander.CommandType.execute_script)) {
                        String response = getResponse(message);
                        if (response.equals("Cant find env variable") || response.equals("Permission to read denied") || response.equals("File not found") ||
                                response.equals("not connected yet")){// переход в демо мод
                            System.out.println(response);
                            System.out.println("The server has no access to Collection. App is turning in demo mode. You can use only 'help' and 'exit' \n " +
                                    "If you want to try to turn on standard mode restart the client app please");
                            tryRead();
                            return readDemo(scanner);
                            //break;
                        } else {
                            System.out.println(response);
                        }
                    }

                }
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("I cant send message");
            }
            byteArrayOutputStream.reset();
        }
        return false;
    }
    public boolean readDemo(Scanner scanner) throws IOException {
        objectOutputStream.flush();
        byteArrayOutputStream.flush();

        boolean exitStatus = false;
        boolean wasEnter = false;
        getResponse(new ExtendMessage(new Message(false), login, password)); // для проверки нажатия на клавишу Enter
        while (!exitStatus) {
            afterConnecting = false;
            String[] text = null;
            Commander.CommandType type = null;
            if (!wasEnter) System.out.println("Enter command");
            wasEnter = false;
            if (scanner.hasNext()) {
                String textline = scanner.nextLine();
                if (textline.trim().isEmpty()) {
                    wasEnter = true;
                    continue;
                }
                text = textline.replaceAll("^\\s+", "").split(" ", 2);
            } else {
                objectOutputStream.writeObject(new ExtendMessage(new Message(true), login, password));
                objectOutputStream.flush();
                System.exit(0);
            }
            String word = text[0];
            boolean normalCommand = true;
            switch (word) {
                case ("help"):
                    type = Commander.CommandType.help;
                    break;
                case ("exit"):
                    exitStatus = true;
                    type = Commander.CommandType.exit;
                    System.exit(0);
                    break;
                case ("mode"):
                    System.out.println("trying");
                    type = Commander.CommandType.mode;
                    break;
                default:
                    System.out.println("You can use only 'help' and 'exit' commands when server has no access to collection");
                    normalCommand = false;
                    break;
            }
            try {
                if (normalCommand) {
                    ExtendMessage message = new ExtendMessage(new Message(new Ticket(), type, null, false), login, password);
                    String response = getResponse(message);
                    if (response.startsWith("help") && type != Commander.CommandType.help) response = "";
                    if (response.equals("Cant find env variable") || response.equals("Permission to read denied") || response.equals("File not found") ||
                            response.equals("not connected yet")) {
                        System.out.println(response);

                        continue;
                    } else {
                        System.out.println(response);
                    }
                    if (type == Commander.CommandType.mode) return true;
                }
            } catch (Exception e) {
                System.out.println("I cant send message");
            }
            byteArrayOutputStream.reset();
        }
        return false;
    }
    public String authorization(Scanner scanner){
        System.out.println("Do you want to sign in or sign up? (type in or up)");
        while (true){
            String answer = fromconsole(scanner);
            if (answer.equals("in")) {
                System.out.println("Enter login:");
                login = fromconsole(scanner);
                System.out.println("Enter password:");
                password = fromconsole(scanner);
                AuthorizateMessage message = new AuthorizateMessage(login, password, true);
                return getResponse(message);
            } else if (answer.equals("up")) {
                System.out.println("Enter login:");
                login = fromconsole(scanner);
                System.out.println("Enter password:");
                password = fromconsole(scanner);
                AuthorizateMessage message = new AuthorizateMessage(login, password, false);
                return getResponse(message);
            } else if (answer.equals("exit")) {
                System.exit(0);
            } else {
                System.out.println("incorrect answer, try again");
            }
        }

    }
    public void execute_script(String argument) throws IOException {
        System.out.println("argument : " + argument);
        try {
            File script = new File(argument);
            read(new Scanner(script), true);
        } catch (IOException | NullPointerException e) {
            System.out.println("Script not found :(");
        }
    }
    public static Ticket inputTicket() throws NumberFormatException {
        Scanner consoleScanner = new Scanner(System.in);
        int exceptionStatus = 0; // для проверки на исключения парсинга и несоответсвия правилам
        System.out.println("Enter name");
        String name = "";
        while (exceptionStatus == 0){
            if (consoleScanner.hasNext()){
                name = consoleScanner.nextLine();
                if ((name != null) && (name.length() > 0)) {
                    exceptionStatus = 1;
                } else {
                    System.out.println("field can't be empty. Try again");
                }
            } else {
                System.exit(0);
            }
        }
        System.out.println("Enter x coordinate (double)");
        Integer x = inputAnyInt();
        System.out.println("Enter y coordinate (Lonf)");
        Long y = inputAnyLong();
        Coordinates coordinates = new Coordinates(x, y);
        System.out.println("Enter price (Double, positive)");
        Integer price = inputPositiveInt();
        System.out.println("Enter TicketType(VIP, USUAL, BUDGETARY, CHEAP)");
        String TicketType = null;
        if (consoleScanner.hasNext()){
            TicketType = consoleScanner.nextLine();
        } else {
            System.exit(0);
        }
        TicketType type = inputTicketType(TicketType);
        int exceptionStat2 = 0;
        System.out.println("Enter EventName ");
        String EventName = null;


        while (exceptionStat2 == 0){
            if (consoleScanner.hasNext()){
                EventName = consoleScanner.nextLine();
                if ((EventName != null) && (EventName.length() > 0)) {
                    exceptionStat2 = 1;
                } else {
                    System.out.println("field can't be empty. Try again");
                }
            } else {
                System.exit(0);
            }
        }

        System.out.println("Enter minAge (int)");
        int minAge = inputPositiveInt();
        System.out.println("Enter ticketCount (long)");
        long ticketCount = inputPositiveLong();
        Integer id = null;
        LocalDateTime data = LocalDateTime.now();
        Event event = new Event(id, EventName, minAge,ticketCount);
        System.out.println("owner is " + login);
        Ticket inputTicket = new Ticket(id, name, coordinates, event, price, type.toString(), data, login );
        return inputTicket;
    }
    /**
     * Method that reads any Long field from console
     * @return Long field
     */
    public static Long inputAnyLong(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Long x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus == 0){
                try {
                    x = Long.parseLong(inputScanner.nextLine());
                    exceptionStatus = 1;
                } catch (NumberFormatException e) {
                    System.out.println("Input must be Long. Try again");
                }
            }
        } else {
            System.exit(0);
        }
        return x;
    }

    /**
     * Method that reads positive Long field from console
     * @return Long field
     */
    public static Long inputPositiveLong(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Long x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus >= 0){
                try {
                    x = Long.parseLong(inputScanner.nextLine());
                    if (x <= 0) {
                        exceptionStatus = 2;
                    } else {
                        exceptionStatus = -1;
                    }
                } catch (NumberFormatException e) {
                    exceptionStatus = 1;
                }
                switch (exceptionStatus) {
                    case (1):
                        System.out.println("Input must be long. Try again.");
                        break;
                    case (2):
                        System.out.println("Input cant be <= 0. Try again");
                        break;
                }
            }
        } else {
            System.exit(0);
        }

        return x;
    }
    /**
     * Method that reads positive Integer field from console
     * @return Integer field
     */
    public static Integer inputPositiveInt(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Integer x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus >= 0){
                try {
                    x = Integer.parseInt(inputScanner.nextLine());
                    if (x < 0) {
                        exceptionStatus = 2;
                    } else {
                        exceptionStatus = -1;
                    }
                } catch (NumberFormatException e) {
                    exceptionStatus = 1;
                }
                switch (exceptionStatus) {
                    case (1):
                        System.out.println("Input must be int. Try again.");
                        break;
                    case (2):
                        System.out.println("Input cant be < 0. Try again");
                        break;
                }
            }
        } else {
            System.exit(0);
        }

        return x;
    }
    /**
     * Method that reads any Double field from console
     * @return Double field
     */
    public static Double inputAnyDouble(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Double x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus == 0){
                try {
                    x = Double.parseDouble(inputScanner.nextLine());
                    exceptionStatus = 1;
                } catch (NumberFormatException e) {
                    System.out.println("Input must be Duble. Try again.");
                }
            }
        } else {
            System.exit(0);
        }
        return x;
    }

    public static Integer inputAnyInt(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Integer x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus == 0){
                try {
                    x = Integer.parseInt(inputScanner.nextLine());
                    exceptionStatus = 1;
                } catch (NumberFormatException e) {
                    System.out.println("Input must be Int. Try again.");
                }
            }
        } else {
            System.exit(0);
        }
        return x;
    }
    /**
     * Method that reads positive Double field from console
     * @return Double field
     */
    public static Double inputPositiveDouble(){
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        Double x = null;
        if (inputScanner.hasNext()){
            while (exceptionStatus >= 0){
                try {
                    x = Double.parseDouble(inputScanner.nextLine());
                    if (x <= 0) {
                        exceptionStatus = 2;
                    } else {
                        exceptionStatus = -1;
                    }
                } catch (NumberFormatException e) {
                    exceptionStatus = 1;
                }
                switch (exceptionStatus) {
                    case (1):
                        System.out.println("Input must be Double. Try again.");
                        break;
                    case (2):
                        System.out.println("Input can't be less than 0. Try again");
                        break;
                }
            }
        } else {
            System.exit(0);
        }
        return x;
    }
    /**
     * Method transform String (received from console) to TicketType. If doesn't match any type then read next string from console
     * @param type String to transform
     * @return received type
     */
    public static TicketType inputTicketType(String type) {
        int exceptionStatus = 0;
        Scanner inputScanner = new Scanner(System.in);
        TicketType ticketType = TicketType.CHEAP;
        while (exceptionStatus == 0){
            switch (type){
                case ("CHEAP"):
                    ticketType = TicketType.CHEAP;
                    exceptionStatus = 1;
                    break;
                case ("VIP"):
                    ticketType = TicketType.VIP;
                    exceptionStatus = 1;
                    break;
                case ("BUDGETARY"):
                    ticketType = TicketType.BUDGETARY;
                    exceptionStatus = 1;
                    break;
                case ("USUAL"):
                    ticketType = TicketType.USUAL;
                    exceptionStatus = 1;
                    break;
                default:
                    System.out.println("Invalid Tickettype. Try again");
                    type = inputScanner.nextLine();
                    break;
            }
        }
        return ticketType;
    }

    private static boolean ArgPars(String s) throws NumberFormatException {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private String fromconsole(Scanner scanner) {
        if (scanner.hasNext()) {
            return scanner.nextLine();
        } else {
            System.exit(0);
        }
        return null;
    }
}
