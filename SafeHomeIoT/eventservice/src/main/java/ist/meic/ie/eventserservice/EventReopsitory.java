package ist.meic.ie.eventserservice;

import ist.meic.ie.events.Event;
import ist.meic.ie.events.EventItem;
import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.parser.ParseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventReopsitory {

    public static List<Event> getEvents(String eventType, int lastReceivedId) throws SQLException, InvalidEventTypeException, ParseException {
        String tableName = "";
        List<Event> eventsToReturn = new ArrayList<>();

        switch (eventType) {
            case "temperature" : tableName = "temperatureMessage"; break;
            case "motion" : tableName = "motionMessage"; break;
            case "smoke" : tableName = "smokeMessage"; break;
            case "image" : tableName = "imageMessage"; break;
            case "video" : tableName = "videoMessage"; break;
        }
        //System.out.println("select * from " + tableName + " where ID > " + lastReceivedId + " and SIMCARD=" + SIMCARD);
        DatabaseConfig dbConfig = new DatabaseConfig(Constants.MEDIATION_DB, "SafeHomeIoTEvents", Constants.PROVISION_DB_USER, Constants.PROVISION_DB_PASSWORD);
        PreparedStatement stmt = dbConfig.getConnection().prepareStatement("SELECT * FROM " + tableName + " WHERE ID > " + lastReceivedId);
        ResultSet events = stmt.executeQuery();
        while (events.next()) {
            int id = events.getInt("id");
            String type = events.getString("type");
            int MSISDN = events.getInt("MSISDN");
            int SIMCARD = events.getInt("SIMCARD");
            Date ts = events.getTimestamp("ts");
            float measurement = 0;
            String description = "";

            if (type.equals("temperature") || type.equals("smoke")) {
                measurement = events.getFloat("measurement");
                description = "";
            }

            if (type.equals("motion") || type.equals("image") || type.equals("video")) {
                measurement = 0;
                description = events.getString("description");
            }

            EventItem eventItem = new EventItem(id, type, SIMCARD, MSISDN, ts, measurement, description);
            eventsToReturn.add(eventItem.getEvent());
        }
        events.close();
        stmt.close();
        return eventsToReturn;
    }
}
