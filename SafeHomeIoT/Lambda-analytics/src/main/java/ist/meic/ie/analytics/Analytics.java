package ist.meic.ie.analytics;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.MissingFormatArgumentException;

import static ist.meic.ie.utils.Constants.*;

public class Analytics implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONObject customer = parseInput(inputStream, logger);
        long customerId = (long) customer.get("customerId");
        long SIMCARD = (long)customer.get("SIMCARD");
        String deviceType= (String) customer.get("deviceType");
        logger.log("Customer id: " + customerId + "\n");
        logger.log("SIMCARD:"+ SIMCARD+"\n");
        logger.log("Device Type: "+deviceType+"\n");

        Connection conn = new DatabaseConfig(COMPANY_EVENTS_DB, "EventReader", COMPANY_EVENTS_DB_USER, COMPANY_EVENTS_PASSWORD).getConnection();

        try {
            String stats= deviceStatistics(deviceType,SIMCARD, conn,outputStream,logger);
            logger.log("Stats obtained :"+stats+"\n");
        }
        catch (Exception e) {
            logger.log(e.toString());
        }
    }


    private JSONObject parseInput(InputStream inputStream, LambdaLogger logger) {
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

            if (event.get("customerId") == null) throw new MissingFormatArgumentException("No customerId defined!");
            if(event.get("SIMCARD") == null) throw new MissingFormatArgumentException("No device defined!");
            if(event.get("deviceType") == null) throw new MissingFormatArgumentException("No device type defined!");
        } catch (Exception e) {
            logger.log(e.toString());
        }
        return event;
    }

    private void buildResponse(OutputStream outputStream, String responseMsg, int statusCode) throws IOException {
        JSONObject responseBody = new JSONObject();
        JSONObject responseJson = new JSONObject();
        JSONObject headerJson = new JSONObject();
        responseBody.put("message", responseMsg);
        responseJson.put("statusCode", statusCode);

        responseJson.put("headers", headerJson);
        responseJson.put("body", responseBody.toString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    private String deviceStatistics(String deviceType, long SIMCARD, Connection conn, OutputStream outputStream,LambdaLogger logger) throws SQLException, IOException {
        String stats =null;
        switch (deviceType){
            case("temperature"):getMeasurementStats("temperature",SIMCARD,conn, outputStream,logger); break;
            case("smoke"):getMeasurementStats("smoke",SIMCARD,conn,outputStream,logger); break;
            case("image"):getDescriptionStats("image",SIMCARD,conn,outputStream,logger);break;
            case("motion"):getDescriptionStats("motion",SIMCARD,conn,outputStream,logger);break;
            case("video"):getDescriptionStats("video",SIMCARD,conn,outputStream,logger);break;
            default:break;
        }
        return stats;
    }

    private String getMeasurementStats(String deviceType, long SIMCARD, Connection conn, OutputStream outputStream, LambdaLogger logger) throws SQLException, IOException {
        PreparedStatement stmt;
        ResultSet rs;
        String statsString=null;
        stmt = conn.prepareStatement("select AVG(measurement),MIN(measurement),MAX(measurement) from " +deviceType+"Message"+" where SIMCARD = ?");
        stmt.setLong(1, SIMCARD);
        rs = stmt.executeQuery();
        if(!rs.next()){
            buildResponse(outputStream, "Device with SIMCARD " + SIMCARD + " has no measurements!", 500);
        }
        else{
            String avg = rs.getString("AVG(measurement)");
            String min = rs.getString("MIN(measurement)");
            String max = rs.getString("MAX(measurement)");

            statsString= "SIMCARD: " + SIMCARD +" deviceType: "+deviceType
                    + " avg: " + avg + " min: " + min + " max: "+ max;
            buildResponse(outputStream,statsString,200);
        }
        rs.close();
        conn.close();
        return statsString;
    }

    private String getDescriptionStats(String deviceType, long SIMCARD, Connection conn, OutputStream outputStream, LambdaLogger logger) throws SQLException, IOException {
        PreparedStatement stmt;
        ResultSet rs;
        String deviceTable=deviceType+"Message";
        stmt = conn.prepareStatement("SELECT COUNT(*) FROM "+deviceTable+" WHERE SIMCARD=? AND description=\"ALARM\";");
        stmt.setLong(1,SIMCARD);
        rs= stmt.executeQuery();
        if(!rs.next()){
            buildResponse(outputStream, "Device with SIMCARD " + SIMCARD + " has no descriptions!", 500);
            return null;
        }
        int numbAlarms= rs.getInt("COUNT(*)");
        stmt = conn.prepareStatement("SELECT description,COUNT(description) AS value_occurrence FROM "+deviceTable+" WHERE SIMCARD=? GROUP BY description ORDER BY value_occurrence DESC LIMIT 1;");
        stmt.setLong(1, SIMCARD);
        rs = stmt.executeQuery();
        if(!rs.next()){
            buildResponse(outputStream, "Device with SIMCARD " + SIMCARD + " has no descriptions!", 500);
            return null;
        }
        String max = rs.getString("description");
        long max_occurrence = rs.getLong("value_occurrence");
        stmt = conn.prepareStatement("SELECT description,COUNT(description) AS value_occurrence FROM "+deviceTable+" WHERE SIMCARD=? GROUP BY description ORDER BY value_occurrence ASC LIMIT 1;");
        stmt.setLong(1, SIMCARD);
        rs=stmt.executeQuery();
        if(!rs.next()){
            buildResponse(outputStream, "Device with SIMCARD " + SIMCARD + " has no descriptions!", 500);
            return null;
        }
        String min = rs.getString("description");
        long min_ocurrence = rs.getLong("value_occurrence");
        String statsString=null;
        statsString="SIMCARD: " + SIMCARD +" deviceType: "+deviceType
                + " max: " + max +" max_occurrence: "+max_occurrence+
                " min: " + min + " min_occurrence: "+ min_ocurrence+
                " numbAlarms: "+ numbAlarms;
        buildResponse(outputStream,statsString,200);
        rs.close();
        conn.close();
        return statsString;
    }
}

