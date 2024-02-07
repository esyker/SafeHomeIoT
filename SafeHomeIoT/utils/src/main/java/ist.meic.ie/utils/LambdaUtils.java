package ist.meic.ie.utils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.MissingFormatArgumentException;

public class LambdaUtils {
    public static JSONObject parseInput(InputStream inputStream, LambdaLogger logger) {
        JSONObject event = null;
        try {
            JSONParser parser = new JSONParser();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JSONObject msg = (JSONObject) parser.parse(reader);
            if (msg.get("body") == null) {
                throw new MissingFormatArgumentException("Missing body field");
            }
            logger.log("input:" + msg.toString());
            event = (JSONObject) parser.parse(msg.get("body").toString());
            logger.log(event.toString());
        } catch (Exception e) {
            logger.log(e.toString());
        }
        return event;
    }

    public static void buildResponse(OutputStream outputStream, String responseMsg, int statusCode) throws IOException {
        JSONObject responseBody = new JSONObject();
        JSONObject responseJson = new JSONObject();
        JSONObject headerJson = new JSONObject();
        responseBody.put("message", responseMsg);
        responseJson.put("statusCode", statusCode);

        responseJson.put("headers", headerJson);
        responseJson.put("body", responseBody.toString());

        System.out.println(responseBody.toJSONString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }
}
