package ist.meic.ie.events;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class SmokeEvent extends Event{

    public SmokeEvent(JSONObject event) throws InvalidEventTypeException {
        super(event);
        if(event.get("measurement") == null)
            throw new InvalidEventTypeException(event.toJSONString());
        this.measurement = ((Double) event.get("measurement")).floatValue();
    }

    public SmokeEvent(float measurement, int SIMCARD, int MSISDN) {
        super("smoke", SIMCARD, MSISDN);
        this.measurement = measurement;
    }

    public SmokeEvent(int id, float measurement, int SIMCARD, int MSISDN, Date ts) {
        super(id, "smoke", SIMCARD, MSISDN, ts);
        this.measurement = measurement;
    }

    public float getMeasurement() {
        return measurement;
    }

    @Override
    public void insertToDb(DatabaseConfig config) {
        try {
            PreparedStatement stmt = config.getConnection().prepareStatement("insert into smokeMessage (SIMCARD, MSISDN, measurement, type) values(?,?,?,?)");
            stmt.setInt(1, this.getSIMCARD());
            stmt.setInt(2, this.getMSISDN());
            stmt.setFloat(3, this.getMeasurement());
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
                "\"measurement\": " + this.getMeasurement() + ",\n" +
                "\"timestamp\": \"" + this.getTimestamp() + "\"\n" +
                "}";
    }

}
