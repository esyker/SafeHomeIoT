package ist.meic.ie.eventserservice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mysql.cj.xdevapi.JsonParser;
import ist.meic.ie.events.Event;
import ist.meic.ie.events.EventItem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import scala.util.parsing.json.JSON;

import java.io.*;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.stream.Collectors;

public class EventService implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        LambdaLogger logger = context.getLogger();
        try {
            JSONParser parser = new JSONParser();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JSONObject msg = (JSONObject) parser.parse(reader);
            if (msg.get("body") == null) {
                throw new MissingFormatArgumentException("Missing body field");
            }
            logger.log("input:" + msg.toString());
            JSONObject event = (JSONObject) parser.parse(msg.get("body").toString());
            logger.log(event.toString());

            if (event.get("eventType") == null) throw new MissingFormatArgumentException("No event type provided!");
            if (event.get("lastReceivedId") == null) throw new MissingFormatArgumentException("No last received provided!");

            String eventType = (String) event.get("eventType");
            int lastReceived = ((Long) event.get("lastReceivedId")).intValue();

            List<Event> events = EventReopsitory.getEvents(eventType, lastReceived);
            List<String> stringEvents = events.stream()
                    .map(Event::toString)
                    .collect(Collectors.toList());

            JSONObject responseBody = new JSONObject();
            JSONObject responseJson = new JSONObject();
            JSONObject headerJson = new JSONObject();
            responseBody.put("message", stringEvents.toString());
            headerJson.put("x-custom-header", "my custom header value");
            responseJson.put("statusCode", 200);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(responseJson.toString());
            writer.close();

        } catch (Exception e) {
            logger.log("Error : " + e.toString());
        }

    }
}
