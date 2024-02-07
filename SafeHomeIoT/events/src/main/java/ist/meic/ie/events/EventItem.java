package ist.meic.ie.events;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Date;
import java.util.Random;

public class EventItem {
    private Event event;

    public EventItem(String jsonString) throws InvalidEventTypeException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject event = (JSONObject) parser.parse(jsonString);
        String type = (String) event.get("type");
        switch (type) {
            case "temperature" : this.event = new TemperatureEvent(event); break;
            case "image" : this.event = new ImageEvent(event); break;
            case "video" : this.event = new VideoEvent(event); break;
            case "smoke" : this.event = new SmokeEvent(event); break;
            case "motion" : this.event = new MotionEvent(event); break;
            default: throw new InvalidEventTypeException("Invalid event type");
        }
    }

    public EventItem(String type, int SIMCARD, int MSISDN) throws InvalidEventTypeException {
        String[] txtMsgs = {"not intrusion", "intrusion"};
        switch (type) {
            case "temperature" : this.event = new TemperatureEvent(-50 + new Random().nextFloat() * (500 - (-50)), SIMCARD, MSISDN); break;
            case "image" : this.event = new ImageEvent(txtMsgs[0 + (int)(Math.random() * ((1 - 0) + 1))], SIMCARD, MSISDN); break;
            case "video" : this.event = new VideoEvent(txtMsgs[0 + (int)(Math.random() * ((1 - 0) + 1))], SIMCARD, MSISDN); break;
            case "smoke" : this.event = new SmokeEvent(-50 + new Random().nextFloat() * (500 - (-50)), SIMCARD, MSISDN); break;
            case "motion" : this.event = new MotionEvent(txtMsgs[0 + (int)(Math.random() * ((1 - 0) + 1))] , SIMCARD, MSISDN); break;
            default: throw new InvalidEventTypeException("Invalid event type");
        }
    }

    public EventItem(int id, String type, int SIMCARD, int MSISDN, Date ts, float measurement, String description) throws InvalidEventTypeException {
        switch (type) {
            case "temperature" : this.event = new TemperatureEvent(id, measurement, SIMCARD, MSISDN, ts); break;
            case "image" : this.event = new ImageEvent(id, description, SIMCARD, MSISDN, ts); break;
            case "video" : this.event = new VideoEvent(id, description, SIMCARD, MSISDN, ts); break;
            case "smoke" : this.event = new SmokeEvent(id, measurement, SIMCARD, MSISDN, ts); break;
            case "motion" : this.event = new MotionEvent(id, description, SIMCARD, MSISDN, ts); break;
            default: throw new InvalidEventTypeException("Invalid event type");
        }
    }

    public Event getEvent() {
        return event;
    }

    public String getType() { return event.getType();}

    public String getDescription() {return event.getDescription();}

    public float getMeasurement() {return event.getMeasurement();}
}