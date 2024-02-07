package ist.meic.ie.eventsreader;

import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ist.meic.ie.utils.Constants;

public class EventReader {
    private DatabaseConfig databaseConfig;
    private String eventTypes []= {"temperature","motion","smoke","image","video"};
    private int lastReceivedIDs [] ={0,0,0,0,0};
    private static JSONParser parser = new JSONParser();

    public Connection getNewConnection(){
        databaseConfig= new DatabaseConfig(Constants.COMPANY_EVENTS_DB,"EventReader",Constants.COMPANY_EVENTS_DB_USER,Constants.COMPANY_EVENTS_PASSWORD);
        return databaseConfig.getConnection();
    }

    public void receiveEvents(){
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
                            case "temperature" : insertTemperatureEvent(eventMessage,conn); break;
                            case "motion" : insertMotionEvent(eventMessage,conn); break;
                            case "smoke" : insertSmokeEvent(eventMessage,conn); break;
                            case "image" : insertImageEvent(eventMessage,conn); break;
                            case "video" : insertVideoEvent(eventMessage,conn); break;
                        }
                    }
                    conn.close();
                }catch (ParseException | SQLException e){
                    System.out.println("\n \"message\" field not found in JSONObject or error with database connection!\n\n");
                    e.printStackTrace();
                }
                //HTTPMessages.postMsg(response,"application/json","alarm.com");
            }
        }
    }

    public void insertImageEvent(JSONObject message, Connection conn) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMotionEvent(JSONObject message, Connection conn){
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertSmokeEvent(JSONObject message, Connection conn){
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTemperatureEvent(JSONObject message, Connection conn) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertVideoEvent(JSONObject message, Connection conn){
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
