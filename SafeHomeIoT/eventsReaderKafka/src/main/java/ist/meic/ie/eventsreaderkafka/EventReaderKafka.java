package ist.meic.ie.eventsreaderkafka;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.*;
import ist.meic.ie.events.*;

public class EventReaderKafka {
    private DatabaseConfig databaseConfig;
    private String eventTypes []= {"temperature","motion","smoke","image","video"};
    private int lastReceivedIDs [] ={0,0,0,0,0};
    private static JSONParser parser = new JSONParser();
    private String kafkaIP;

    public EventReaderKafka(String kafkaIP) {
        this.kafkaIP=kafkaIP;
    }

    public Connection getNewConnection(){
        databaseConfig= new DatabaseConfig(Constants.COMPANY_EVENTS_DB,"EventReader",Constants.COMPANY_EVENTS_DB_USER,Constants.COMPANY_EVENTS_PASSWORD);
        return databaseConfig.getConnection();
    }

    public void receiveEvents(){
        KafkaProducer<String, String> producer = KafkaConfig.createKafkaProducer(kafkaIP);
        String eventType;
        int lastReceivedID;
        while (true) {
            for(int i=0;i<5;i++){
                eventType=eventTypes[i];
                lastReceivedID=0;//lastReceivedIDs[i];
                JSONObject event = new JSONObject();
                event.put("eventType",eventType);
                event.put("lastReceivedId",lastReceivedID);
                JSONObject response = HTTPMessages.getMsg(event,"application/json","getnextevent.com");
                if(response==null) {
                    System.out.println("\nHTTP Message does not have code 200!");
                    continue;
                }
                try {
                    Connection conn = getNewConnection();
                    JSONArray eventMessages = (JSONArray) parser.parse(response.get("message").toString());
                    lastReceivedIDs[i]+=eventMessages.size();
                    Iterator<JSONObject> it= eventMessages.iterator();
                    while(it.hasNext()){
                        JSONObject eventMessage=it.next();
                        switch (eventType)
                        {
                            case "temperature" : insertTemperatureEvent(eventMessage,conn,producer); break;
                            case "motion" : insertMotionEvent(eventMessage,conn,producer); break;
                            case "smoke" : insertSmokeEvent(eventMessage,conn,producer); break;
                            case "image" : insertImageEvent(eventMessage,conn,producer); break;
                            case "video" : insertVideoEvent(eventMessage,conn,producer); break;
                        }
                    }
                    conn.close();
                }catch (ParseException | SQLException e){
                    System.out.println("\n \"message\" field not found in JSONObject or error with database connection!\n\n");
                    e.printStackTrace();
                }
            }
        }
    }

    public void insertImageEvent(JSONObject message, Connection conn, KafkaProducer producer) {
        try {
            long SIMCARD = (long) message.get("SIMCARD");
            long MSISDN = (long) message.get("MSISDN");
            String timestamp = (String) message.get("timestamp");
            String Description = (String) message.get("description");
            String Type = (String) message.get("type");
            PreparedStatement stmt = conn.prepareStatement("insert into imageMessage (SIMCARD, MSISDN, description, type) values(?,?,?,?)");
            stmt.setLong(1, SIMCARD);
            stmt.setLong(2, MSISDN);
            stmt.setString(3, Description);
            stmt.setString(4, Type);
            stmt.execute();
            ImageEvent event = new ImageEvent(message);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("events-messages", event.getType(), event.toString());
            producer.send(producerRecord);
        } catch (SQLException | InvalidEventTypeException e) {
            e.printStackTrace();
        }
    }

    public void insertMotionEvent(JSONObject message, Connection conn, KafkaProducer producer){
        try {
            long SIMCARD = (long) message.get("SIMCARD");
            long MSISDN = (long) message.get("MSISDN");
            String timestamp = (String) message.get("timestamp");
            String Description = (String) message.get("description");
            String Type = (String) message.get("type");
            PreparedStatement stmt = conn.prepareStatement("insert into motionMessage (SIMCARD, MSISDN, description, type) values(?,?,?,?)");
            stmt.setLong(1, SIMCARD);
            stmt.setLong(2, MSISDN);
            stmt.setString(3, Description);
            stmt.setString(4, Type);
            stmt.execute();
            MotionEvent event = new MotionEvent(message);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("events-messages", event.getType(), event.toString());
            producer.send(producerRecord);
        } catch (SQLException | InvalidEventTypeException e) {
            e.printStackTrace();
        }
    }

    public void insertSmokeEvent(JSONObject message, Connection conn, KafkaProducer producer){
        try {
            long SIMCARD = (long) message.get("SIMCARD");
            long MSISDN = (long) message.get("MSISDN");
            String timestamp = (String) message.get("timestamp");
            double Measurement = (double) message.get("measurement");
            String Type = (String) message.get("type");
            PreparedStatement stmt = conn.prepareStatement("insert into smokeMessage (SIMCARD, MSISDN, measurement, type) values(?,?,?,?)");
            stmt.setLong(1, SIMCARD);
            stmt.setLong(2, MSISDN);
            stmt.setDouble(3, Measurement);
            stmt.setString(4, Type);
            stmt.execute();
            SmokeEvent event = new SmokeEvent(message);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("events-messages", event.getType(), event.toString());
            producer.send(producerRecord);
        } catch (SQLException | InvalidEventTypeException e) {
            e.printStackTrace();
        }
    }

    public void insertTemperatureEvent(JSONObject message, Connection conn, KafkaProducer producer) {
        try {
            long SIMCARD = (long) message.get("SIMCARD");
            long MSISDN = (long) message.get("MSISDN");
            String timestamp = (String) message.get("timestamp");
            double Measurement = (double) message.get("measurement");
            String Type = (String) message.get("type");
            PreparedStatement stmt = conn.prepareStatement("insert into temperatureMessage (SIMCARD, MSISDN, measurement, type) values(?,?,?,?)");
            stmt.setLong(1, SIMCARD);
            stmt.setLong(2, MSISDN);
            stmt.setDouble(3, Measurement);
            stmt.setString(4, Type);
            stmt.execute();
            TemperatureEvent event = new TemperatureEvent(message);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("events-messages", event.getType(), event.toString());
            producer.send(producerRecord);
        } catch (SQLException | InvalidEventTypeException e) {
            e.printStackTrace();
        }
    }

    public void insertVideoEvent(JSONObject message, Connection conn, KafkaProducer producer){
        try {
            long SIMCARD = (long) message.get("SIMCARD");
            long MSISDN = (long) message.get("MSISDN");
            String timestamp = (String) message.get("timestamp");
            String Description = (String) message.get("description");
            String Type = (String) message.get("type");
            PreparedStatement stmt = conn.prepareStatement("insert into videoMessage (SIMCARD, MSISDN, description, type) values(?,?,?,?)");
            stmt.setLong(1, SIMCARD);
            stmt.setLong(2, MSISDN);
            stmt.setString(3, Description);
            stmt.setString(4, Type);
            stmt.execute();
            VideoEvent event = new VideoEvent(message);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("events-messages", event.getType(), event.toString());
            producer.send(producerRecord);
        } catch (SQLException | InvalidEventTypeException e) {
            e.printStackTrace();
        }
    }
}
