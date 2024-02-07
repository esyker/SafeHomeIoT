package ist.meic.ie.msisdn.getstatus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.MissingFormatArgumentException;

public class GetStatusMSISDN implements RequestStreamHandler {

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

            //"getStatus"://{"SIMCARD":913123123}
            simcard = ((Long) event.get("SIMCARD")).intValue();
            String status = getStatus(simcard, logger);
            LambdaUtils.buildResponse(outputStream, status, 200);

        } catch (Exception e) {
            logger.log("Error" + e);
        }
    }

    public String getStatus(int simcard, LambdaLogger logger){

        DatabaseConfig dbConfig = new DatabaseConfig(Constants.MEDIATION_DB, "MSISDNStatus", Constants.MEDIATION_DB_USER, Constants.MEDIATION_DB_PASSWORD);
        //DatabaseConfig dbConfig = new DatabaseConfig("mytestdb2.cwoffguoxxn0.us-east-1.rds.amazonaws.com", "MSISDNStatus", "storemessages", "enterpriseintegration2021");

        PreparedStatement statusActive;
        ResultSet queryactive;
        String status = "NOT FOUND";
        try {
            statusActive = dbConfig.getConnection().prepareStatement ("select * from Status where SIMCARD = ? having ts >= all (select ts from Status where SIMCARD = ?)");
            statusActive.setInt(1, simcard);
            statusActive.setInt(2, simcard);
            queryactive = statusActive.executeQuery();

            if (queryactive.next()) {
                Date ts = queryactive.getTimestamp("ts");
                //Dates are stored in a different timezone. Therefore we substract 1 hour to compensate
                Date fiveMinutesAgo =  new Date(System.currentTimeMillis() - 60000 * 5);
                logger.log(ts.toString() + "\n");
                logger.log(fiveMinutesAgo.toString() + "\n");
                if (ts.before(fiveMinutesAgo)) {
                    status = "UNKNOWN";
                } else {
                    status = queryactive.getString("status").equals("1") ? "UP" : "DOWN";
                }
            }
            statusActive.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return status;
    }
}
