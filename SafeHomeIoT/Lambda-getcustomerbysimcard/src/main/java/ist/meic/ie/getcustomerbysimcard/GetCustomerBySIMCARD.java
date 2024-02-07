package ist.meic.ie.getcustomerbysimcard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetCustomerBySIMCARD implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);

        if(obj.get("SIMCARD") == null) {
            LambdaUtils.buildResponse(outputStream, "No SIMCARD provided!", 500);
            return;
        }

        int SIMCARD = ((Long) obj.get("SIMCARD")).intValue();

        JSONObject customerObj = new JSONObject();
        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        PreparedStatement stmt;
        ResultSet rs;

        try {
            stmt = conn.prepareStatement("SELECT * FROM Customer, Device WHERE SIMCARD = ? AND customerId = id");
            stmt.setInt(1, SIMCARD);
            rs = stmt.executeQuery();

            if (rs.next()) {
                customerObj.put("customerId", rs.getString("id"));
                customerObj.put("firstName", rs.getString("firstName"));
                customerObj.put("lastName", rs.getString("lastName"));
                customerObj.put("postalCode", rs.getString("postalCode"));
                customerObj.put("street", rs.getString("street"));
                customerObj.put("district", rs.getString("district"));
                customerObj.put("council", rs.getString("council"));
                customerObj.put("parish", rs.getString("parish"));
                customerObj.put("doorNumber", rs.getInt("doorNumber"));
            }
            rs.close();
            stmt.close();

            LambdaUtils.buildResponse(outputStream, customerObj.toJSONString(), 200);

        } catch (Exception e) {
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException throwables) {
                logger.log(throwables.toString());
            }
        }

    }


}
