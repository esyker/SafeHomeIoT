package ist.meic.ie.paysubscription;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;
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

public class PaySubscription implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);
        int subId = 0;

        if(obj.get("customerId") == null) {
            LambdaUtils.buildResponse(outputStream, "Customer Id not provided", 500);
            return;
        }

        if(obj.get("creditCardNumber") == null) {
            LambdaUtils.buildResponse(outputStream, "Credit card not provided", 500);
            return;
        }

        if(obj.get("value") == null) {
            LambdaUtils.buildResponse(outputStream, "Value not provided", 500);
            return;
        }

        int customerId = ((Long) obj.get("customerId")).intValue();

        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        PreparedStatement stmt, stmt2;
        ResultSet rs;
        try {
            if (checkCustomer(outputStream, customerId, conn)) return;
            if (checkSub(outputStream, customerId, conn)) return;
            stmt = conn.prepareStatement("SELECT * FROM Device WHERE customerId = ?");
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            while(rs.next()) {
                JSONObject simcardObj = new JSONObject();
                int SIMCARD = rs.getInt("SIMCARD");
                simcardObj.put("SIMCARD", SIMCARD);
                simcardObj.put("MSISDN", 0);
                simcardObj.put("deviceType", "");
                HTTPMessages.postMsg(simcardObj, "application/json", "activatesimcard.com", logger);

                stmt2 = conn.prepareStatement("UPDATE Device SET status = ? WHERE SIMCARD = ?");
                stmt2.setString(1, "ACTIVE");
                stmt2.setInt(2, SIMCARD);
                stmt2.executeUpdate();
                stmt2.close();
            }
            rs.close();
            stmt.close();


            stmt = conn.prepareStatement("SELECT * FROM CustomerSubscriptions WHERE customerId = ?");
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                subId = rs.getInt("subscriptionId");
            }
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("UPDATE Subscription SET status = ? WHERE id = ?");
            stmt.setString(1, "ACTIVE");
            stmt.setInt(2, subId);
            stmt.executeUpdate();
            stmt.close();

            LambdaUtils.buildResponse(outputStream, "Activated customer " + customerId + " subscription!", 200);
        } catch (Exception e) {
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

    }

    private boolean checkCustomer(OutputStream outputStream, int customerId, Connection conn) throws SQLException, IOException {
        ResultSet rs;
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT * FROM Customer WHERE id = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        if(!rs.next()) {
            LambdaUtils.buildResponse(outputStream, "Customer with " + customerId + "does not exist!", 200);
            conn.close();
            return true;
        }
        rs.close();
        stmt.close();
        return false;
    }

    private boolean checkSub(OutputStream outputStream, int customerId, Connection conn) throws SQLException, IOException {
        ResultSet rs;
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT * FROM Subscription, CustomerSubscriptions WHERE Subscription.id = CustomerSubscriptions.subscriptionId AND CustomerSubscriptions.customerId = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        if(rs.next()) {
            if(rs.getString("status").equals("ACTIVE")) {
                LambdaUtils.buildResponse(outputStream, "Customer " + customerId + "'s subscription is already paid!", 200);
                conn.close();
                return true;
            }
        }
        rs.close();
        stmt.close();
        return false;
    }
}
