package ist.meic.ie.deletecustomer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteCustomer implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);

        if(obj.get("customerId") == null) {
            LambdaUtils.buildResponse(outputStream, "Customer Id not provided", 500);
            return;
        }

        int customerId = ((Long) obj.get("customerId")).intValue();

        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();

        PreparedStatement stmt;
        ResultSet rs;
        try {
            logger.log("1");
            if (checkCustomer(outputStream, customerId, conn)) return;
            logger.log("2");

            if (deleteSimcards(outputStream, logger, customerId, conn)) return;
            logger.log("3");

            if (deleteSubscription(outputStream, logger, customerId)) return;
            logger.log("4");

            deleteDevices(customerId, conn);
            logger.log("5");

            deleteCustomer(customerId, conn);
            logger.log("6");

            LambdaUtils.buildResponse(outputStream, "Customer " + customerId + "deleted!",200);
        } catch(Exception e) {
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private boolean deleteSimcards(OutputStream outputStream, LambdaLogger logger, int customerId, Connection conn) throws SQLException, IOException {
        PreparedStatement stmt;
        ResultSet rs;
        stmt = conn.prepareStatement("SELECT * FROM Device WHERE customerId = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        while(rs.next()) {
            JSONObject simcardObj = new JSONObject();
            simcardObj.put("SIMCARD", rs.getInt("SIMCARD"));
            if(HTTPMessages.postMsg(simcardObj, "application/json", "deletesimcard.com", logger) != 200) {
                LambdaUtils.buildResponse(outputStream, "Could not delete simcard!", 500);
                return true;
            }
        }
        rs.close();
        stmt.close();
        return false;
    }

    private void deleteCustomer(int customerId, Connection conn) throws SQLException {
        PreparedStatement stmt;
        stmt = conn.prepareStatement("DELETE FROM Customer WHERE id = ?");
        stmt.setInt(1, customerId);
        stmt.executeUpdate();
        stmt.close();
    }

    private boolean deleteSubscription(OutputStream outputStream, LambdaLogger logger, int customerId) throws IOException {
        JSONObject customerIdObj = new JSONObject();
        customerIdObj.put("customerId", customerId);
        if(HTTPMessages.postMsg(customerIdObj, "application/json", "cancelsubscriptionlambda.com", logger) != 200) {
            LambdaUtils.buildResponse(outputStream, "Could not cancel subscription!",500);
            return true;
        }
        return false;
    }

    private void deleteDevices(int customerId, Connection conn) throws SQLException {
        PreparedStatement stmt;
        stmt = conn.prepareStatement("DELETE FROM Device WHERE customerId = ?");
        stmt.setInt(1, customerId);
        stmt.executeUpdate();
        stmt.close();
    }

    private boolean checkCustomer(OutputStream outputStream, int customerId, Connection conn) throws SQLException, IOException {
        ResultSet rs;
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT * FROM Customer WHERE id = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        if(!rs.next()) {
            LambdaUtils.buildResponse(outputStream, "Customer with id " + customerId + " does not exist!", 500);
            return true;
        }
        rs.close();
        stmt.close();
        return false;
    }
}
