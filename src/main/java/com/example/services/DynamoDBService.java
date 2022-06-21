package com.example.services;

import com.example.entities.TimeEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.io.StringWriter;
import java.util.*;

import static java.time.LocalTime.now;

// Uses the DynamoDB API to read/write to a DynamoDB table.
@Component
public class DynamoDBService{
    private DynamoDbClient getClient() {
        Region region = Region.US_WEST_2;
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(region)
                .build();
        return ddb;
    }

    // Get single entry from DynamoDB table based on Key
    public String getItem(String id) {
        DynamoDbClient ddb = getClient();
        String status = "";
        String description = "";

        HashMap<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();
        keyToGet.put("id", AttributeValue.builder().s(id).build());
        GetItemRequest req = GetItemRequest.builder()
                .key(keyToGet)
                .tableName("SimpleTimeClock")
                .build();
        try {
            Map<String, AttributeValue> returnedItem = ddb.getItem(req).item();
            for (Map.Entry<String, AttributeValue> entry : returnedItem.entrySet()) {
                String k = entry.getKey();
                AttributeValue v = entry.getValue();

                if (k.compareTo("description")==0) {
                    description = v.s();
                } else if (k.compareTo("status")==0) {
                    status = v.s();
                }
            }
            return convertToString(toXmlItem(id, description, status));
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    // Retrieve entries from DynamoDB table
    public ArrayList<TimeEntry> getListItems() {
        DynamoDbEnhancedClient ec = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getClient())
                .build();

        try {
            DynamoDbTable<Time> custTable = ec.table("Time", TableSchema.fromBean(Time.class));
            Iterator<Time> results = custTable.scan().items().iterator();
            TimeEntry timeEntry;
            ArrayList<TimeEntry> itemList = new ArrayList();

            while (results.hasNext()) {
                timeEntry = new TimeEntry();
                Time time = results.next();
                timeEntry.setId(time.getId());
                timeEntry.setDescription(time.getDescription());
                timeEntry.setStatus(time.getStatus());
                timeEntry.setName(time.getName());
                timeEntry.setGuide(time.getGuide());
                timeEntry.setDate(time.getDate());

                itemList.add(timeEntry);
            }
            return itemList;
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Done");
        return null;
    }

    // Archives based on item key
    public String archiveItem(String id) {
        DynamoDbClient ddb = getClient();
        HashMap<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();
        itemKey.put("id", AttributeValue.builder().s(id).build());

        // Updates columns
        HashMap<String, AttributeValueUpdate> updatedItem = new HashMap<String, AttributeValueUpdate>();
        updatedItem.put("archive", AttributeValueUpdate.builder().value(AttributeValue.builder().s("Closed").build()).action(AttributeAction.PUT).build());

        UpdateItemRequest req = UpdateItemRequest.builder()
                .tableName("SimpleTimeClock")
                .key(itemKey)
                .attributeUpdates(updatedItem)
                .build();

        try {
            ddb.updateItem(req);
            return "Archived successfully";
        } catch (ResourceNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    // Updates based on item key
    public String archiveTime(String id) {
        DynamoDbClient ddb = getClient();

        try {
            DynamoDbEnhancedClient ec = DynamoDbEnhancedClient.builder().dynamoDbClient(getClient()).build();

            DynamoDbTable<Time> table = ec.table("SimpleTimeClock", TableSchema.fromBean(Time.class));

            Key key = Key.builder().partitionValue(id).build();
            Time time = table.getItem(r->r.key(key));
            time.setArchive("Closed");

            table.updateItem(r->r.item(time));
            return "Archived successfully";
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    // Updates items in the table
    public String updateItem(String id, String status) {
        DynamoDbClient ddb = getClient();
        HashMap<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();
        itemKey.put("id", AttributeValue.builder().s(id).build());
        HashMap<String, AttributeValueUpdate> updatedItem = new HashMap<String, AttributeValueUpdate>();

        updatedItem.put("status", AttributeValueUpdate.builder().value(AttributeValue.builder().s(status).build()).action(AttributeAction.PUT).build());
        UpdateItemRequest req = UpdateItemRequest.builder()
                .tableName("SimpleTimeClock")
                .key(itemKey)
                .attributeUpdates(updatedItem)
                .build();
        try {
            ddb.updateItem(req);
            return "Updated status successfully";
        } catch (ResourceNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    // Get open items from table
    public String getOpenItems() {
        DynamoDbEnhancedClient ec = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getClient())
                .build();

    try {
        DynamoDbTable<Time> table = ec.table("SimpleTimeClock", TableSchema.fromBean(Time.class));
        AttributeValue attribute = AttributeValue.builder().s("Open").build();
        Map<String, AttributeValue> myMap = new HashMap<>();
        myMap.put(":val1", attribute);
        Map<String, String> myExMap = new HashMap<>();
        myExMap.put("#archive", "archive");

        Expression expression = Expression.builder().expressionValues(myMap).expressionNames(myExMap).expression("#archive = :val1").build();
        ScanEnhancedRequest er = ScanEnhancedRequest.builder().filterExpression(expression).limit(15).build();

        Iterator<Time> results = table.scan(er).items().iterator();
        TimeEntry timeEntry;
        ArrayList<TimeEntry> itemList = new ArrayList();

        while (results.hasNext()) {
            timeEntry = new TimeEntry();
            Time time = results.next();
            timeEntry.setId(time.getId());
            timeEntry.setDescription(time.getDescription());
            timeEntry.setStatus(time.getStatus());
            timeEntry.setName(time.getName());
            timeEntry.setGuide(time.getGuide());
            timeEntry.setDate(time.getDate());

            itemList.add(timeEntry);
        }
        return convertToString(toXml(itemList));
    } catch (DynamoDbException e) {
        System.err.println(e.getMessage());
        System.exit(1);
    }
    System.out.println("Done");
    return "";
    }

    // Get closed items from table
    public String getClosedItems() {
        DynamoDbEnhancedClient ec = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getClient())
                .build();

        try {
            DynamoDbTable<Time> table = ec.table("SimpleTimeClock", TableSchema.fromBean(Time.class));
            AttributeValue attribute = AttributeValue.builder().s("Closed").build();
            Map<String, AttributeValue> myMap = new HashMap<>();
            myMap.put(":val1", attribute);
            Map<String, String> myExMap = new HashMap<>();
            myExMap.put("#archive", "archive");

            Expression expression = Expression.builder().expressionValues(myMap).expressionNames(myExMap).expression("#archive = :val1").build();
            ScanEnhancedRequest er = ScanEnhancedRequest.builder().filterExpression(expression).limit(15).build();

            Iterator<Time> results = table.scan(er).items().iterator();
            TimeEntry timeEntry;
            ArrayList<TimeEntry> itemList = new ArrayList();

            while (results.hasNext()) {
                timeEntry = new TimeEntry();
                Time time = results.next();
                timeEntry.setId(time.getId());
                timeEntry.setDescription(time.getDescription());
                timeEntry.setStatus(time.getStatus());
                timeEntry.setName(time.getName());
                timeEntry.setGuide(time.getGuide());
                timeEntry.setDate(time.getDate());

                itemList.add(timeEntry);
            }
            return convertToString(toXml(itemList));
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Done");
        return "";
    }

    // Put an item to the table
    public void setItem(TimeEntry item) {
        DynamoDbEnhancedClient ec = DynamoDbEnhancedClient.builder().dynamoDbClient(getClient()).build();

        putRecord(ec, item);
    }

    // Put a record into the table
    public void putRecord(DynamoDbEnhancedClient ec, TimeEntry item) {
        try {
            DynamoDbTable<Time> table = ec.table("SimpleTimeClock", TableSchema.fromBean(Time.class));
            LocalDate localDate = LocalDate.parse("2022-02-24");
            LocalDateTime localDateTime = localDate.atStartOfDay();
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
            String myGuid = UUID.randomUUID().toString();

            Time record = new Time();
            record.setId(myGuid);
            record.setDescription(item.getDescription());
            record.setStatus(item.getStatus());
            record.setName(item.getName());
            record.setGuide(item.getGuide());
            record.setDate(now());
            record.setArchive("Open");

            table.putItem(record);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    // Convert to XML
    private Document toXml(List<TimeEntry> itemList) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = (Document) docBuilder.newDocument();
            Element rootElement = doc.createElement("Items");
            doc.appendChild(rootElement);
            int count = itemList.size();

            for (int i = 0; i < count; i++) {
                TimeEntry items = itemList.get(i);

                Element item = doc.createElement("Item");
                rootElement.appendChild(item);

                Element id = doc.createElement("Id");
                id.appendChild(doc.createTextNode(items.getId()));
                item.appendChild(id);

                Element description = doc.createElement("Description");
                description.appendChild(doc.createTextNode(items.getDescription()));
                item.appendChild(description);

                Element status = doc.createElement("Status");
                status.appendChild(doc.createTextNode(items.getStatus()));
                item.appendChild(status);

                Element name = doc.createElement("Name");
                name.appendChild(doc.createTextNode(items.getName()));
                item.appendChild(name);

                Element guide = doc.createElement("Guide");
                guide.appendChild(doc.createTextNode(items.getGuide()));
                item.appendChild(guide);

                Element date = doc.createElement("Date");
                date.appendChild(doc.createTextNode(items.getDate()));
                item.appendChild(date);
            }
            return doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String convertToString(Document xml) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

        private String now() {
            String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
            return sdf.format(cal.getTime());
        }

        private Document toXmlItem(String id2, String desc2, String status2) {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = (Document) docBuilder.newDocument();

                Element rootElement = doc.createElement("Items");
                doc.appendChild(rootElement);
                Element itemElement = doc.createElement("Item");
                doc.appendChild(itemElement);
                Element id = doc.createElement("Id");
                id.appendChild(doc.createTextNode(id2));
                rootElement.appendChild(id);
                Element description = doc.createElement("Description");
                description.appendChild(doc.createTextNode(desc2));
                rootElement.appendChild(description);
                Element status = doc.createElement("Status");
                status.appendChild(doc.createTextNode(status2));
                rootElement.appendChild(status);

                return doc;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        return null;
    }
}

