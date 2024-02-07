package ist.meic.ie.msisdn.suspend;

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

public class SuspendMSISDN  implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context){
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

            if (event.get("SIMCARD") == null) throw new MissingFormatArgumentException("No SIM Card defined!");
            simcard = ((Long) event.get("SIMCARD")).intValue();
            boolean suspended = suspend(simcard);

            JSONObject responseBody = new JSONObject();
            JSONObject responseJson = new JSONObject();
            JSONObject headerJson = new JSONObject();
            if (suspended) {
                responseBody.put("message","SIMCARD " + simcard + " suspended");
                responseJson.put("statusCode", 200);
            } else {
                responseBody.put("message","No SIMCARD " + simcard + " exists");
                responseJson.put("statusCode", 500);
            }

            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(responseJson.toString());
            writer.close();
        } catch (Exception e) {
            logger.log("Error" + e);
        }
    }

    public boolean suspend(int simcard) {
        //DatabaseConfig dbConfig = new DatabaseConfig("mytestdb2.cwoffguoxxn0.us-east-1.rds.amazonaws.com", "HLR", "storemessages", "enterpriseintegration2021");
        DatabaseConfig dbConfig  = new DatabaseConfig(Constants.PROVISION_DB, "HLR", Constants.PROVISION_DB_USER, Constants.PROVISION_DB_PASSWORD);
        boolean suspended = false;
        try {
            Statement select = dbConfig.getConnection().createStatement();
            ResultSet rs = select.executeQuery("SELECT * FROM Subscriber WHERE SIMCARD = " + simcard);
            if(rs.next()) {
                PreparedStatement update = dbConfig.getConnection().prepareStatement("UPDATE Subscriber SET state = ? WHERE SIMCARD = " + simcard);
                update.setString(1, "SUSPENDED");
                update.executeUpdate();
                update.close();
                suspended = true;
            }
            dbConfig.getConnection().close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return suspended;
    }
}