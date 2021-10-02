import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Random;
public class Ticket implements Serializable {
    private Integer id;
    private String  name;
    public Coordinates coordinates;
    public Event event;
    public long price ;
    public TicketType type;
    private String owner;
    public LocalDateTime creationDate;
    public boolean marker = false;
    public static final long serialVersionUID = 18L;
    public Ticket(Integer id, String name, Coordinates coordinates, Event e, long price, String t, LocalDateTime creationDate,String owner) {
        this.id = id;
        if (id == null) {
           this.id = new Random().nextInt();
        }
        if ((name != null) && (name.length() != 0)) {
            this.name = name;
        } else {
            throw new IncorrectFieldException("Incorrect input name");
        }
        if (coordinates != null) {
            this.coordinates = coordinates;
        } else {
            throw new IncorrectFieldException("Incorrect input coordinates");
        }
        this.creationDate = creationDate;
        if (creationDate == null) this.creationDate = LocalDateTime.now();
        if (price > 0) {
            this.price = price;
        } else {
            throw new IncorrectFieldException("Incorrect input price (need to be >0)");
        }
        switch (t){
            case "BUDGETARY":
                this.type = TicketType.BUDGETARY;
                break;
            case "CHEAP":
                this.type = TicketType.CHEAP;
                break;
            case "USUAL":
                this.type = TicketType.USUAL;
                break;
            case "VIP":
                this.type = TicketType.VIP;
                break;
            default: throw new IncorrectFieldException("Incorrect input TicketType");
        }
        this.event = e;
        if (event == null) {
            throw new IncorrectFieldException("Incorrect input event");
        }
        this.owner = owner;

    }
    public Ticket(){
        this.id = 1;
        this.name = "default";
        this.coordinates = new Coordinates(1, 5L);
        this.event = new Event(4,"default",1,1);
        this.price = 1;
        this.creationDate = LocalDateTime.now();
        this.type = TicketType.USUAL;
    }

    public Ticket(boolean marker) {
        super();
        this.marker = marker;
    }

    public Ticket(Integer id, String name, Coordinates coords, Event event, long price, TicketType type, LocalDateTime creationDate,  String owner) {
    }

    public Integer getId() {
        return id;
    }
    public long getPrice(){
        return price;
    }
    public String getName() {
        return name;
    }
    public boolean getMarker() {return marker; }
    public java.time.LocalDateTime getCreationDate() {
        return creationDate;
    }
    public Coordinates getCoords() {
        return coordinates;
    }
    public TicketType getType(){
        return type;
    }
    public String getOwner() {return owner; }

    public Event getEvent(){
        return event;
    }
    public void setId(Integer id){
        this.id = id;
    }
    public void update(Ticket ticket){
        this.id = ticket.getId();
        this.name = ticket.getName();
        this.coordinates = ticket.getCoords();
        this.event = ticket.getEvent();
        this.type = ticket.getType();
        this.creationDate = ticket.getCreationDate();
        this.price = ticket.getPrice();
    }
    @Override
    public String toString() {
        return "["+id+" " +name+" " + " "+coordinates.getX()+" "+ coordinates.getY()+" " + " " +event.getIdTicket() + " " + event.getNameTicket() + " " + event.getMinAge()+ " " + event.getTicketsCount() +  " " + price + " "+ type + owner+"]";
    }
}
class Event implements Serializable{
    private Integer id;
    private String name;
    private int minAge;
    private long ticketsCount;
    private static final long serialVersionUID = 18L;


    public Event(Integer id, String name, int minAge, long ticketsCount){
        this.id = id;
        if (id == null) {
            this.id = new Random().nextInt();
        }


        if ((name != null) && (name.length() != 0)) {
            this.name = name;
        } else {
            throw new NumberFormatException();
        }

        this.minAge = minAge;

        if (ticketsCount > 0) {
            this.ticketsCount = ticketsCount;
        } else {
            throw new NumberFormatException();
        }

    }

    public Integer getIdTicket(){return id;}
    public String getNameTicket(){return name;}
    public Integer getMinAge(){return minAge;}
    public int getMinAge2(){return minAge;}
    public Long getTicketsCount(){return ticketsCount;}
    public void setIdEvent(Integer id){
        this.id = id;
    }
    @Override
    public String toString() {
        return "["+id+"_"+name+"_"+minAge+"_"+ticketsCount + "]";
    }
}
class Coordinates implements Serializable {

    public Integer x;
    public Long y;
    private static final long serialVersionUID = 18L;

    public Coordinates(Integer x, Long y) {
        if (x != null) {
            this.x = x;
        } else {
            throw new IncorrectFieldException("Incorrect input X");
        }
        if (y != null) {
            this.y = y;
        } else {
            throw new IncorrectFieldException("Incorrect input Y");
        }
    }

    public Integer getX() {
        return x;
    }

    public Long getY() {
        return y;
    }

    @Override
    public String toString() {
        return "x:" + x +
                ", y:" + y;
    }
}
enum TicketType implements Serializable {
    VIP,
    USUAL,
    BUDGETARY,
    CHEAP;
}
