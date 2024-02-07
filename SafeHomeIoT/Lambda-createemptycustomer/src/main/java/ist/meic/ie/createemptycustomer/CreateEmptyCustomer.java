package ist.meic.ie.createemptycustomer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;

public class CreateEmptyCustomer implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONObject newCustomer = LambdaUtils.parseInput(inputStream, logger);
        int customerId = 0;
        PreparedStatement stmt;
        ResultSet rs;
        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling", Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        try {
            stmt = conn.prepareStatement("INSERT INTO Customer (firstName) VALUES ('TBD')", Statement.RETURN_GENERATED_KEYS);
            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if(rs.next()) {
                customerId = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        LambdaUtils.buildResponse(outputStream, ""+customerId, 200);
    }
}
