import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Commander {
    String argument = "";
    CopyOnWriteArrayList<Ticket> TicketCollection;
    boolean exitStatus = false;
    final boolean fromScript;
    private final DataOutputStream outputStream;
    private CommandType type = CommandType.help;
    Ticket ticket;
    Logger logger = Logger.getLogger("server.command");
    String login = null;
    String password = null;
    DBManager dbManager;
    public Commander(DataOutputStream outputStream, String argument, Ticket ticket, DBManager dbManager,  boolean fromScript, String login, String password) {
        this.outputStream = outputStream;
        this.argument = argument;
        this.dbManager = dbManager;
        try {
            this.TicketCollection = dbManager.readCollection();
        } catch (Exception e){
            e.printStackTrace();
        }
        this.fromScript = fromScript;
        this.ticket = ticket;
        this.login = login;
        this.password = password;
    }

    public void ArgumentChanger(String argument) {
        this.argument = argument;
    }
    public void TypeChanger(CommandType type) {
        this.type = type;
    }

    public void run(Executor outPool) throws IOException{
        logger.info("running command");
        outPool.execute(() -> {
            try {
                switch (type) {
                    case help: this.help();break;
                    case info: this.info();break;
                    case show: this.show();break;
                    case clear: this.clear();break;
                    case exit: this.exit();break;
                    case add: this.add();break;
                    case unique_type: this.unique_type();break;
                    case insert_at: this.insert_at();break;
                    case add_if_min: this.add_if_min();break;
                    case average_price: this.average_price();break;
                    case mode: this.mode();break;
                    case add_if_max: this.add_if_max();break;
                    case count_by_price: this.count_by_price();break;
                    case update: this.update();break;
                    case remove_by_id: this.remove_by_id();break;
                    case shuffle: this.shuffle();break;
                }

            }   catch (NullPointerException ignored) {} catch (IOException e) {
                e.printStackTrace();
            }
        });

    }
    public void mode() throws IOException {
        System.out.println("here");
        logger.info("'mode' command trying to get access");
        boolean isMarker = false;
        for (Ticket ticket : TicketCollection) { if (ticket.getMarker()) isMarker = true;}
        if (isMarker) {
            outputStream.writeUTF("not connected yet");
            logger.info("Not connected yet, answer sent");
        } else {
            outputStream.writeUTF("connection set!");
        }
    }


    public void help() throws IOException {
        logger.info("'help' command was detected");
        outputStream.writeUTF("help : вывести справку по доступным командам\n" +
                "info : вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)\n" +
                "show : вывести в стандартный поток вывода все элементы коллекции в строковом представлении\n" +
                "add {element} : добавить новый элемент в коллекцию\n"+
                "update id {element} : обновить значение элемента коллекции, id которого равен заданному\n"+
                "remove_by_id : удалить элемент из коллекции по его id\n" +
                "clear : очистить коллекцию\n"+  "exit : закончить сессию\n" +
                "save : сохранить коллекцию в файл\n"+
                "insert_at index {element} : добавить новый элемент в заданную позицию\n"+
                "add_if_min {element} : добавить новый элемент в коллекцию, если его значение меньше, чем у наименьшего элемента этой коллекции\n"+
                "shuffle : перемешать элементы коллекции в случайном порядке\n"+
                "average_of_price : вывести среднее значение поля price для всех элементов коллекции\n"+
                "count_by_price price : вывести количество элементов, значение поля price которых равно заданному\n"+
                "unique_type : вывести уникальные значения поля event всех элементов в коллекции\n");
    }
    public void info() throws IOException {
        logger.info("'info' command was detected");
        outputStream.writeUTF("type = Vector of Tickets \n Number of items = " + TicketCollection.size());
        logger.info("Answer was sent");
    }
    public void show() throws IOException {
        logger.info("'show' command was detected");
        String description = "";
        for (Ticket ticket : TicketCollection) {
            description += extendedDescription(ticket) + "\n";
        }
        outputStream.writeUTF(description);
        logger.info("Answer was sent");
    }
    public void clear() throws IOException {
        logger.info("'clear' command was detected");
        try {
            outputStream.writeUTF("cleared");
            TicketCollection.clear();
            dbManager.update(TicketCollection);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println("1");
        }
    }
    public void shuffle() throws IOException {
        logger.info("'shuffle' command was detected");
        try {
            outputStream.writeUTF("Collection was shuffled");
            Collections.shuffle(TicketCollection);
            System.out.println(TicketCollection.get(0).getName()+"0");
            System.out.println(TicketCollection.size());
            dbManager.update(TicketCollection);
            System.out.println("___" +TicketCollection.get(0).getName()+" A");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println("12");
        }
    }
    public void exit() throws IOException {
        logger.info("'exit' command was detected");
        outputStream.writeUTF("This session finished");
        //save();
        exitStatus = true;
        outputStream.flush();
    }

    public void unique_type() throws IOException{
        logger.info("'unique_type' command was detected");
        try{
            List<String> unique = new LinkedList<>();
            for (Ticket ticket: TicketCollection){
                unique.add(ticket.getType().toString());
            }
            Set<String> uniqueElement = new HashSet<String>(unique);
            outputStream.writeUTF("Unique elements of eventType: " + uniqueElement.toString());
        } catch (Exception e){
            outputStream.writeUTF("Try again, ");
        }

    }
    public void add() throws IOException {
        logger.info("'add' command was detected");
        try {
            TicketCollection.add(ticket);
            logger.info("as");
            dbManager.update(TicketCollection);
            outputStream.writeUTF("new Ticket element was added");
            logger.info("Answer was sent");
        } catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket 1");
            e.printStackTrace();
        }
    }

    public void remove_by_id() throws IOException {
        logger.info("'remove_by_id' command was detected");
        try {
            int id_argument = Integer.parseInt(argument);
            if (TicketCollection.stream().map(Ticket::getId).anyMatch(id -> id == id_argument)) {
                int x = TicketCollection.size();
                TicketCollection.removeIf(d ->  d.getOwner().equals(login) & d.getId() == id_argument ); //d.getId() == id_argument |
                int y = TicketCollection.size();
                if (y<x){
                    outputStream.writeUTF("Element(s) has been removed");
                } else outputStream.writeUTF("Element(s) hasn't been removed. You aren't an owner");
                dbManager.update(TicketCollection);

                logger.info("Answer was sent");
            } else {
                outputStream.writeUTF("No such element in TicketCollection");
                logger.info("Answer was sent");
            }
        } catch (NumberFormatException e){
            outputStream.writeUTF("Invalid argument. Try again");
            logger.info("Answer was sent");
        } catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket");
        }
    }
    public void insert_at() throws IOException {
        logger.info("'insert_at' command was detected");
        try {
            int place = Integer.parseInt(argument);
            if (TicketCollection.size()>=place) {
                TicketCollection.add(place , ticket);
                dbManager.update(TicketCollection);
                outputStream.writeUTF("Element(s) has been inserted");
                logger.info("Answer was sent");
            } else {
                outputStream.writeUTF("No such element in TicketCollection");
                logger.info("Answer was sent");
            }
        } catch (NumberFormatException e){
            outputStream.writeUTF("Invalid argument. Try again");
            logger.info("Answer was sent");
        }catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket");
        }
    }
    public void add_if_max() throws IOException {
        logger.info("'add_if_min' command was detected");
        try {
            if (TicketCollection.stream().map(Ticket::getPrice).allMatch(price -> price < ticket.getPrice())) {
                TicketCollection.add(ticket);
                dbManager.update(TicketCollection);
                outputStream.writeUTF("new Ticket has been added");
            } else {
                outputStream.writeUTF("new Ticket has NOT been added");
            }
            logger.info("Answer was sent");
        } catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket");
        }
    }

    public void update() throws IOException {
        logger.info("'update' command was detected");
        try {
            int id_argument = Integer.parseInt(argument);
            if (TicketCollection.stream().map(Ticket::getId).anyMatch(id -> id == id_argument)) {
                TicketCollection.stream().filter(d -> d.getId() == id_argument).forEach(d -> d.update(ticket));
                dbManager.update(TicketCollection);
                outputStream.writeUTF("Ticket has been updated");
                logger.info("Answer was sent");
            } else {
                outputStream.writeUTF("No such element id in TicketCollection. Try 'show' to see available id's");
                logger.info("Answer was sent");
            }

        } catch (NumberFormatException e){
            outputStream.writeUTF("Invalid argument. Try again");
            logger.info("Answer was sent");
        } catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket");
        }
    }

    public void add_if_min() throws IOException {
        logger.info("'add_if_min' command was detected");
        try {
            if (TicketCollection.stream().map(Ticket::getPrice).allMatch(price -> price > ticket.getPrice())) {
                TicketCollection.add(ticket);
                dbManager.update(TicketCollection);
                outputStream.writeUTF("new Ticket has been added");
            } else {
                outputStream.writeUTF("new Ticket has NOT been added");
            }
            logger.info("Answer was sent");
        }  catch (SQLException e) {
            outputStream.writeUTF("Connection failed. Can't add ticket");
        }
    }


    public void average_price() throws IOException{
        logger.info("'average_price' command was detected");

        int x = 0;
        for (Ticket ticket:TicketCollection){
            x+=ticket.getPrice();
        }
        int average = x/TicketCollection.size();
        String average_price = Integer.toString(average);
        outputStream.writeUTF(average_price);
    }


    public void count_by_price() throws IOException {
        try {
            int arg_price = Integer.parseInt(argument);
            if (TicketCollection.stream().map(Ticket::getPrice).anyMatch(price -> price == arg_price)) {
                double output = TicketCollection.stream().filter(d -> d.getPrice() == arg_price).count();
                String nums = Double.toString(output);
                outputStream.writeUTF(nums);
            } else {
                outputStream.writeUTF("No such elements");
                logger.info("Answer was sent");
            }
            logger.info("'count_by_price' command was detected");
            logger.info("Answer was sent");
        } catch (NumberFormatException e){
            outputStream.writeUTF("Invalid argument. Try again");
            logger.info("Answer was sent");
        }
    }
    public void save() throws  IOException {
        logger.info("saving to disk");
        File Input = null;
        Scanner scan = null;
        try {
            Input = new File(System.getenv("Input"));      // проверка на наличие переменной окружения
            scan = new Scanner(Input);
        } catch (NullPointerException e) {
            System.out.println("Cant find env variable");
            System.exit(0);
        }catch (FileNotFoundException e) {   // неправильный путь к файлу или нет доступа на чтение
            System.out.println("File not found");
            System.exit(0);
        }
        try {
            FileWriter writter = new FileWriter(Input);
            writter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<Ticket>\n");
            for (Ticket ticket : TicketCollection){
                String id =  ticket.getId().toString();
                String name = ticket.getName();
                String coordinates = ticket.getCoords().getX() + " " + ticket.getCoords().getY();
                String idevent = ticket.event.getIdTicket().toString();
                String eventname = ticket.event.getNameTicket();
                String eventage = ticket.event.getMinAge().toString();
                String eventcount = ticket.event.getTicketsCount().toString();
                String ticketdate = ticket.getCreationDate().toString();
                String typeField = ticket.type.toString();
                String price = Double.toString(ticket.getPrice());
                writter.write("<ticket " + "id='"+id+"'"+" name='"+name+"' "+"coordinates='"+coordinates+"' "+"eventid='"+idevent+"' "
                        + "eventname='"+eventname+"' "+" eventage='"+eventage+"' "+" eventcount='"+eventcount+"' "
                        +" creation_date='"+ticketdate+"' "+" type='"+typeField+"' "+" price='"+price+"' "+" />\n");
            }
            writter.write("</Ticket>");
            writter.close();
            System.out.println("The command was executed");
            //outputStream.writeUTF("You were disconnected by the ADMIN. Please close this Session and start again, if you want");
            logger.info("Collection was saved to disk");
        } catch (NullPointerException e) {
            logger.info("Collection was not saved to disk");
        }
    }

    /**
     * Method used when 'show' command is called
     * @param ticket ticket which description need to be shown
     * @return String description
     */
    public static String extendedDescription(Ticket ticket) {
        return Stream.of(ticket.getId(), ticket.getCoords().getX(), ticket.getCoords().getY(), ticket.getName(),
                ticket.getPrice(), ticket.getCreationDate(), ticket.getType(), ticket.getEvent().getTicketsCount(), ticket.getEvent().getIdTicket(),
                ticket.getEvent().getMinAge(), ticket.getEvent().getNameTicket(), ticket.getOwner()).map(Object::toString).collect(Collectors.joining(", "));
    }

    public enum CommandType {
        help,info,show,add, update,remove_by_id,clear,insert_at,execute_script, add_if_min,
        shuffle,mode,average_price,count_by_price,add_if_max ,exit, unique_type
    }
}
