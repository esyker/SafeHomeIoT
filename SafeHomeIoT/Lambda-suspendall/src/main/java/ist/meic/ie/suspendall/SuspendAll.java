package ist.meic.ie.suspendall;

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

public class SuspendAll implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);


        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            stmt = conn.prepareStatement("UPDATE Subscription SET status = ?");
            stmt.setString(1, "SUSPENDED");
            stmt.executeUpdate();
            stmt.close();

            stmt = conn.prepareStatement("UPDATE Device SET status = ?");
            stmt.setString(1, "SUSPENDED");
            stmt.executeUpdate();
            stmt.close();

            stmt = conn.prepareStatement("SELECT * FROM Device");
            rs = stmt.executeQuery();
            logger.log("Suspending simcards");
            while(rs.next()) {
                JSONObject simcardObject = new JSONObject();
                simcardObject.put("SIMCARD", rs.getInt("SIMCARD"));
                HTTPMessages.postMsg(simcardObject, "application/json", "suspendsimcard.com", logger);
            }
            rs.close();
            stmt.close();
            LambdaUtils.buildResponse(outputStream, "Suspended all subscriptions!", 200);
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
}
