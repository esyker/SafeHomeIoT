package ist.meic.ie.msisdn.activate;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.MissingFormatArgumentException;

public class ActivateMSISDN implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        DatabaseConfig dbConfig = new DatabaseConfig(Constants.PROVISION_DB, "HLR", Constants.PROVISION_DB_USER, Constants.PROVISION_DB_PASSWORD);
        // DatabaseConfig dbConfig = new DatabaseConfig("mytestdb2.cwoffguoxxn0.us-east-1.rds.amazonaws.com", "HLR", "storemessages", "enterpriseintegration2021");

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

            int simcard;
            int msisdn;
            String deviceType = "";

            //"activate"://{"MSISDN":"12312312","SIMCARD":"913123123","deviceType":"temperature"}
            if (event.get("SIMCARD") == null) throw new MissingFormatArgumentException("No SIM Card defined!");
            if (event.get("MSISDN") == null) throw new MissingFormatArgumentException("No MSISDN defined!");
            if (event.get("deviceType") == null) throw new MissingFormatArgumentException("No Device Type defined!");


            simcard = ((Long) event.get("SIMCARD")).intValue();
            msisdn = ((Long) event.get("MSISDN")).intValue();
            deviceType = (String) event.get("deviceType");
            String action = activate(dbConfig, simcard, msisdn, deviceType, logger);

            JSONObject responseBody = new JSONObject();
            JSONObject responseJson = new JSONObject();
            JSONObject headerJson = new JSONObject();
            responseBody.put("message","New SIMCARD " + simcard + " " + action);
            headerJson.put("x-custom-header", "my custom header value");
            responseJson.put("statusCode", 200);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(responseJson.toString());
            writer.close();
        } catch (Exception e) {
            logger.log("Error" + e);
        }
    }

    public String activate(DatabaseConfig dbConfig, int simcard, int msisdn, String deviceType, LambdaLogger logger) throws SQLException {
        String action = "inserted";
        try {
            Statement stmt = dbConfig.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Subscriber WHERE SIMCARD = " + simcard);
            if (!rs.next()) {
                PreparedStatement insert = dbConfig.getConnection().prepareStatement ("INSERT INTO  Subscriber (SIMCARD,MSISDN, deviceType, state) VALUES (?,?,?,?)");
                insert.setInt(1,simcard);
                insert.setInt(2,msisdn);
                insert.setString(3, deviceType);
                insert.setString(4, "ACTIVE");
                insert.executeUpdate();
                insert.close();
                action = "inserted";
            } else {
                PreparedStatement update = dbConfig.getConnection().prepareStatement ("UPDATE Subscriber SET state = ? WHERE SIMCARD = " + simcard);
                update.setString(1,"ACTIVE");
                update.executeUpdate();
                update.close();
                action = "updated";
            }
            dbConfig.getConnection().close();

        } catch (SQLException e) {
            logger.log("Error" + e);
        }
        return action;
    }
}
