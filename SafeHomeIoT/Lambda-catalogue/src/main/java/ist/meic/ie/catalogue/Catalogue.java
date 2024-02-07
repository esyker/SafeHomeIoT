package ist.meic.ie.catalogue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Catalogue implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);


        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        PreparedStatement stmt;
        ResultSet rs;
        JSONObject response = new JSONObject();
        try {
            stmt = conn.prepareStatement("SELECT * FROM Service");
            rs = stmt.executeQuery();
            JSONArray services = new JSONArray();
            while(rs.next()) {
                JSONObject s = new JSONObject();
                s.put("name", rs.getString("name"));
                s.put("cost", rs.getInt("cost"));
                services.add(s);
            }
            rs.close();
            stmt.close();

            response.put("services", services);
            LambdaUtils.buildResponse(outputStream, response.toJSONString(), 200);

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
