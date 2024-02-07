package ist.meic.ie.events;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class ImageEvent extends Event{

    public ImageEvent(JSONObject event) throws InvalidEventTypeException {
        super(event);
        if(event.get("description") == null)
            throw new InvalidEventTypeException(event.toJSONString());
        this.description = (String) event.get("description");
    }


    public ImageEvent(String description, int SIMCARD, int MSISDN) {
        super("image", SIMCARD, MSISDN);
        this.description = description;
    }


    public ImageEvent(int id, String description, int SIMCARD, int MSISDN, Date ts) {
        super(id, "image", SIMCARD, MSISDN, ts);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void insertToDb(DatabaseConfig config) {
        try {
            PreparedStatement stmt = config.getConnection().prepareStatement("insert into imageMessage (SIMCARD, MSISDN, description, type) values(?,?,?,?)");
            stmt.setInt(1, this.getSIMCARD());
            stmt.setInt(2, this.getMSISDN());
            stmt.setString(3, this.getDescription());
            stmt.setString(4, this.getType());
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "{\n" +
                "\"type\": \"" + this.getType() + "\",\n" +
                "\"SIMCARD\": " + this.getSIMCARD() + ",\n" +
                "\"MSISDN\": " + this.getMSISDN() + ",\n" +
                "\"description\": \"" + this.getDescription() + "\",\n" +
                "\"timestamp\": \"" + this.getTimestamp() + "\"\n" +
                "}";
    }
}
