import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
public class CollectionParser {
    File file;
    // DataOutputStream outputStream;
    private final Logger logger = Logger.getLogger("server.fileCollectionReader");
    public CollectionParser(){
    }
    public Vector<Ticket> readCollection() throws ParserConfigurationException, SAXException, IOException {
        FileInputStream fileInputStream = null;
        try {
            String Input = System.getenv("Input");
            fileInputStream = new FileInputStream(Input);
        } catch (NullPointerException e) {
            System.out.println("Cant find env variable");
            System.exit(0);
        }catch (FileNotFoundException e){
            System.out.println("File not found");
            System.exit(0);
        }
        String xmlString = "";
        BufferedInputStream bf = new BufferedInputStream(fileInputStream);
        BufferedReader r = new BufferedReader(
                new InputStreamReader(bf, StandardCharsets.UTF_8));
        String x;
        while((x=r.readLine())!=null){
            xmlString+=x;
            xmlString+=System.lineSeparator();
        }
        Vector<Ticket> TicketCollection = new Vector<>();
        LocalDateTime data = LocalDateTime.now();
        //parse(TicketCollection, xmlString);
        IdChanger(TicketCollection);
        List l = Collections.synchronizedList(new ArrayList());
        //CopyOnWriteArrayList<> l2 = new CopyOnWriteArrayList();
        return (TicketCollection);
    }


    public void parse(List<Object> TicketCollection, String xmlString ) throws IOException, SAXException, ParserConfigurationException {
        if (xmlString.length() > 0) {// Получение фабрики, чтобы после получить билдер документов
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Получили из фабрики билдер, который парсит XML, создает структуру Document в виде иерархического дерева.
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Запарсили XML, создав структуру Document. Теперь у нас есть доступ ко всем элементам, каким нам нужно.
            Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
            // Получение списка всех элементов  внутри корневого элемента (getDocumentElement возвращает ROOT элемент XML файла).
            NodeList ticketElements = document.getDocumentElement().getElementsByTagName("ticket");
            // Перебор всех элементов
            Vector<Integer> ErrorsString = new Vector<>();
            boolean errors = false;
            for (int i = 0; i < ticketElements.getLength(); i++) {
                Node ticket = ticketElements.item(i);
                // Получение атрибутов каждого элемента
                NamedNodeMap attributes = ticket.getAttributes();
                LocalDateTime creationDate = null;
                try {
                    creationDate = LocalDateTime.parse(attributes.getNamedItem("creation_date").getNodeValue());
                } catch (DateTimeParseException e) {
                    errors = true;
                    ErrorsString.add(i);
                } catch (NullPointerException e) {
                    creationDate = LocalDateTime.now();
                }
                Integer id = null;
                try {         // проверяем есть ли id и считываем их
                    id = Integer.parseInt(attributes.getNamedItem("id").getNodeValue());
                } catch (NullPointerException e) {
                    id = null;
                } catch (NumberFormatException e) {
                    errors = true;
                    ErrorsString.add(i);
                }
                Integer idEvent = null;
                try {         // проверяем есть ли id и считываем их
                    idEvent = Integer.parseInt(attributes.getNamedItem("eventid").getNodeValue());
                } catch (NumberFormatException e) {
                    errors = true;
                    ErrorsString.add(i);
                }catch (NullPointerException e) {
                    idEvent = null;
                }
                try{
                    String name = attributes.getNamedItem("name").getNodeValue();
                    Integer l = Integer.parseInt(attributes.getNamedItem("coordinates").getNodeValue().split(" ")[0]);
                    Coordinates coordinates = new Coordinates(l, Long.parseLong(attributes.getNamedItem("coordinates").getNodeValue().split(" ")[1]));
                    String eventname = attributes.getNamedItem("eventname").getNodeValue();
                    Integer eventage = Integer.parseInt(attributes.getNamedItem("eventage").getNodeValue());
                    Long eventcount = Long.parseLong(attributes.getNamedItem("eventcount").getNodeValue());
                    String type = attributes.getNamedItem("type").getNodeValue();
                    Integer price = Integer.parseInt(attributes.getNamedItem("price").getNodeValue());
                    Event event = new Event(idEvent, eventname, eventage, eventcount);
                    TicketCollection.add(new Ticket(id, name, coordinates, event, price, type, creationDate, "somebody"));
                }catch (Exception e){
                    errors = true;
                    ErrorsString.add(i);
                }
            }
            if (errors==true) {
                List<String> unique = new LinkedList<>();
                for (Integer integer : ErrorsString){
                    unique.add(integer.toString());
                }
                Set<String> uniqueElement = new HashSet<String>(unique);
                System.out.println("Invalid fields of elements were found. These elements will not be added to collection: " + uniqueElement);
            }
        }
    }
    public static void IdChanger(Vector<Ticket> tickets){
        LinkedHashSet<Integer> newid = new LinkedHashSet<>();
        for (Ticket ticket : tickets) {
            if (!newid.add(ticket.getId())) {
                ticket.setId(new Random().nextInt());
            }
        }
    }
}