package ist.meic.ie.msisdn.delete;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.KafkaConfig;
import ist.meic.ie.utils.ZookeeperConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.*;
import java.util.MissingFormatArgumentException;
import java.util.Properties;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.MissingFormatArgumentException;
import java.util.Properties;

public class DeleteMSISDN {


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
            int simcard;
            if (event.get("SIMCARD") == null) throw new MissingFormatArgumentException("No SIM Card defined!");
            simcard = ((Long) event.get("SIMCARD")).intValue();

            //"delete"://{SIMCARD:"913123123"}
            boolean deleted = delete(simcard);
            JSONObject responseBody = new JSONObject();
            JSONObject responseJson = new JSONObject();
            JSONObject headerJson = new JSONObject();
            if (deleted) {
                responseBody.put("message","SIMCARD " + simcard + " deleted");
                responseJson.put("statusCode", 200);
            } else {
                responseBody.put("message","No SIMCARD " + simcard + " exists");
                responseJson.put("statusCode", 200);
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

    public boolean delete(int simcard) {
        DatabaseConfig dbConfig = new DatabaseConfig(Constants.PROVISION_DB, "HLR", Constants.PROVISION_DB_USER, Constants.PROVISION_DB_PASSWORD);
        //DatabaseConfig dbConfig = new DatabaseConfig("mytestdb2.cwoffguoxxn0.us-east-1.rds.amazonaws.com", "HLR", "storemessages", "enterpriseintegration2021");
        PreparedStatement delete;
        boolean deleted = false;
        int deletedRows = 0;
        try {
            delete = dbConfig.getConnection().prepareStatement ("delete from Subscriber where SIMCARD=?");
            delete.setInt(1, simcard);
            deletedRows =  delete.executeUpdate();
            delete.close();
            dbConfig.getConnection().close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return deletedRows > 0;
    }
}
