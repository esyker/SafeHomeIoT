package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class Event implements Serializable {
    int id;
    String type;
    int MSISDN;
    int SIMCARD;
    Date timestamp;
    float measurement;
    String description;

    public Event(@JsonProperty("id") int id,@JsonProperty("type") String type,@JsonProperty("MSISDN") int MSISDN,
                 @JsonProperty("SIMCARD") int SIMCARD,@JsonProperty("timestamp") Date timestamp,
                 @JsonProperty("measurement") float measurement,@JsonProperty("description") String description) {
        this.id=id;
        this.type=type;
        this.MSISDN=MSISDN;
        this.SIMCARD=SIMCARD;
        this.timestamp=timestamp;
        this.measurement=measurement;
        this.description=description;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getMSISDN() {
        return MSISDN;
    }

    public int getSIMCARD() {
        return SIMCARD;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public float getMeasurement() {
        return measurement;
    }

    public String getDescription() {
        return description;
    }
}
