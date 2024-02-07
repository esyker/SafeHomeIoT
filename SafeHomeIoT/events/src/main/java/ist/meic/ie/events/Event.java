package ist.meic.ie.events;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.JSONObject;

import java.util.Date;

public abstract class Event {
    private int id;
    private String type;
    private int MSISDN;
    private int SIMCARD;
    private Date timestamp;
    protected float measurement;
    protected String description;

    public Event(JSONObject event) throws InvalidEventTypeException {
        if(event.get("type") == null || event.get("MSISDN") == null || event.get("SIMCARD") == null)
            throw new InvalidEventTypeException(event.toJSONString());
        this.type = (String) event.get("type");
        this.MSISDN = ((Long) event.get("MSISDN")).intValue();
        this.SIMCARD = ((Long) event.get("SIMCARD")).intValue();
    }

    @Deprecated
    public Event(String type, int SIMCARD, int MSISDN) {
        this.type = type;
        this.SIMCARD = SIMCARD;
        this.MSISDN = MSISDN;
    }

    public Event(int id, String type, int SIMCARD, int MSISDN, Date timestamp) {
        this.id = id;
        this.type = type;
        this.SIMCARD = SIMCARD;
        this.MSISDN = MSISDN;
        this.timestamp = timestamp;
    }

    public int getMSISDN() {
        return MSISDN;
    }

    public void setMSISDN(int MSISDN) {
        this.MSISDN = MSISDN;
    }

    public int getSIMCARD() {
        return SIMCARD;
    }

    public void setSIMCARD(int SIMCARD) {
        this.SIMCARD = SIMCARD;
    }

    public String getType() { return type; }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public abstract void insertToDb(DatabaseConfig config);

    public abstract String toString();

    public String getDescription() {
        return description;
    }

    public float getMeasurement() {
        return measurement;
    }
}
