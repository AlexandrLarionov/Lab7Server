import org.postgresql.util.PSQLException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.postgresql.Driver;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;


public class DBManager {
    String DB_URL = "jdbc:postgresql://pg:5432/studs";
    String USER = "s313316";
     String PASS = "nln161";
//    String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
  //String USER = "postgres";
    //String PASS = "nln161";

    String password;
    String login;
    Connection connection = null;

    public DBManager(String login, String password) {
        this.password = password;
        this.login = login;
    }
    public DBManager() { }

    public boolean connect() throws SQLException {                // подключаемся к БД
        try {
            Driver driver = new org.postgresql.Driver();
            DriverManager.registerDriver(driver);
            //Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.out.println("PostgreSQL JDBC Driver is not found. Include it in your library path, reconnecting...");
            //e.printStackTrace();
            return false;
        }
        System.out.println("PostgreSQL JDBC Driver successfully connected");
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Connection to DB failed, reconnecting...");
            //e.printStackTrace();
            return false;
        }
        if (connection != null) {
            System.out.println("You successfully connected to database now");
            return true;
        } else {
            System.out.println("Failed to make connection to database, reconnecting...");
            return false;
        }
    }

    public synchronized CopyOnWriteArrayList<Ticket> readCollection() throws SQLException {// считываем из БД в коллекцию
        CopyOnWriteArrayList<Ticket> TicketCollection = new  CopyOnWriteArrayList<>();
        PreparedStatement statement2 =
                connection.prepareStatement(
                        "SELECT * FROM tickets"
                );

        String selectTableSQL = "SELECT * FROM tickets";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(selectTableSQL);
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String name = rs.getString("name");
                Integer x = rs.getInt("x");

                Long y = rs.getLong("y");

                Coordinates coordinates = new Coordinates(x,y);
                LocalDateTime date = rs.getDate("creationdate").toLocalDate().atTime(0, 0);
                long price = rs.getLong("price");
                Integer type = rs.getInt("type");
                String ticketType = intToTicketType(type);

                Integer idEvent = rs.getInt("idevent");
                String nameEvent = rs.getString("nameevent");
                int minAge = rs.getInt("minage");
                long ticketCount = rs.getLong("ticketcount");

                Event event = new Event(idEvent,nameEvent, minAge, ticketCount);
                String owner = rs.getString("login");
        //        System.out.println(id.toString() +"_" +name+x.toString()+"_"+y.toString()+"_"+
          //              price+"_"+type.toString()+"_"+idEvent.toString()+"_"+nameEvent+"_"+ticketType.toString()+"_"
            //    +minAge+ticketCount+"_____"+date.toString());
                Ticket addedTicket = new Ticket(id, name, coordinates, event, price, "VIP", date, owner);
                if(CommandsRealize.validate(addedTicket)) {
                    TicketCollection.add(addedTicket);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return TicketCollection;
    }
    public synchronized void update(CopyOnWriteArrayList<Ticket> TicketCollection) throws SQLException {// по коллекции обновляем БД
        CopyOnWriteArrayList<Ticket> previousSet = readCollection();// сравниваем сеты и все что отличается добавляем в БД
        for (Ticket ticket : previousSet) {
            deleteTicket(ticket.getId());
        }
        for (Ticket ticket : TicketCollection) {
            addTicket(ticket);
        }
    }
    public synchronized HashMap<String, String> readUserHashMap() {             // получаем список юзеров
        String selectTableSQL = "SELECT login, password FROM users";
        HashMap<String, String> users = new HashMap<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(selectTableSQL);
            while (rs.next()) {
                String login = rs.getString("login");
                String password = rs.getString("password");
                users.put(login, password);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return users;
    }

    public synchronized void addUser(String login, String password) {        // регистрация нового юзера
        String insertTableSQL = "INSERT INTO users" + "(login, password) " +
                "VALUES('" + login + "', '" + password + "')";
        String newTable = "CREATE TABLE IF NOT EXISTS tickets\n" +
                "(\n" +
                "   id integer NOT NULL DEFAULT nextval('some_sequence'),\n" +
                "    name character varying,\n" +
                "\tx integer,\n" +
                "\ty bigserial,\n" +
                "\tcreationdate date,\n" +
                "\tprice bigserial,\n" +
                "\ttype integer,\n" +
                "\tidevent integer,\n" +
                "\tnameevent character varying,\n" +
                "\tminage integer,\n" +
                "\tticketcount bigserial,\n" +
                "\tlogin character varying\n";
    String sequence =  "CREATE SEQUENCE tickets_id_seq\n" +
            "     INCREMENT BY 1\n" +
            "     NO MAXVALUE\n" +
            "     NO MINVALUE\n" +
            "     CACHE 1;";


        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(insertTableSQL);
            System.out.println("New user added!");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    public synchronized void deleteTicket(Integer id) throws SQLException {
        String deleteTableSQL = "DELETE FROM tickets WHERE id = " + id + ";";
        Statement statement = connection.createStatement();
        statement.executeUpdate(deleteTableSQL);
    }

    public synchronized void addTicket(Ticket ticket) throws SQLException {        // регистрация нового юзера
        String insertTableSQL = "INSERT INTO tickets" + "( name, x, y, creationdate, price, " +
                "type, idevent, nameevent, minage,ticketcount,login) " +
                "VALUES ('" + ticket.getName() + //+ ticket.getId() + "', '" +
                "', '" + ticket.getCoords().getX() + "', '" + ticket.getCoords().getY() +
                "', '" + ticket.getCreationDate() + "', '" + ticket.getPrice() + "', '" + dragonTypeToInt(ticket.getType()) +
                "', '" + ticket.getEvent().getIdTicket() + "', '" + ticket.getEvent().getNameTicket() + "', '" + ticket.getEvent().getMinAge() +
                "', '" + ticket.getEvent().getTicketsCount() + "', '" + ticket.getOwner() + "');";



        Statement statement = connection.createStatement();


        statement.executeUpdate(insertTableSQL);


    }

    private String intToTicketType(Integer i) {
        if (i == 1) {
            return "CHEAP";
        } else if (i == 2) {
            return "VIP";
        } else if (i == 3) {
            return "USUAL";
        } else {
            return "BUDGETARY";
        }
    }
    public Integer dragonTypeToInt(TicketType type) {
        if (type == TicketType.CHEAP) {
            return 1;
        } else if (type == TicketType.VIP) {
            return 2;
        }else if (type == TicketType.USUAL) {
            return 3;
        } else {
            return 4;
        }
    }
}