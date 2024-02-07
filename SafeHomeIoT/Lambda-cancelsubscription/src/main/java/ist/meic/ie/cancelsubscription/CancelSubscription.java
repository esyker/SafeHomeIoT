package ist.meic.ie.cancelsubscription;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.MissingFormatArgumentException;

import static ist.meic.ie.utils.Constants.API_KEY;
import static ist.meic.ie.utils.Constants.KONG_ENDPOINT;

public class CancelSubscription implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONObject customer = parseInput(inputStream, logger);
        int customerId = ((Long) customer.get("customerId")).intValue();
        logger.log("Customer id: " + customerId + "\n");

        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling","pedro", "123456789").getConnection();
        try {
            conn.setAutoCommit(false);
            PreparedStatement stmt, stmt2;
            ResultSet rs;
            int subscriptionId;
            if (checkForSubscription(outputStream, logger, customerId, conn)) return;
            suspendDevicesAssociatedWithCustomer(logger, customerId, conn);
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException throwables) {
                logger.log(throwables.toString());
            }
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(e.toString());
            }
        }
    }

    private void suspendDevicesAssociatedWithCustomer(LambdaLogger logger, int customerId, Connection conn) throws SQLException {
        PreparedStatement stmt;
        PreparedStatement stmt2;
        ResultSet rs;
        // SUSPEND ALL USER DEVICES
        stmt = conn.prepareStatement("SELECT * FROM Device WHERE customerId = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        logger.log("Suspending SIMCARD!");
        while(rs.next()) {
            JSONObject jsonObject = new JSONObject();
            int SIMCARD = rs.getInt("SIMCARD");
            jsonObject.put("SIMCARD", SIMCARD);
            postMsg(jsonObject, "application/json", "suspendsimcard.com", logger);
            stmt2 = conn.prepareStatement("UPDATE Device SET status = ? WHERE SIMCARD = ?");
            stmt2.setString(1, "SUSPENDED");
            stmt2.setInt(2, SIMCARD);
            stmt2.executeUpdate();
            stmt2.close();

        }
        rs.close();
        stmt.close();
    }

    private boolean checkForSubscription(OutputStream outputStream, LambdaLogger logger, int customerId, Connection conn) throws SQLException, IOException {
        PreparedStatement stmt;
        PreparedStatement stmt2;

        int subscriptionId;
        ResultSet rs, rs2;
        stmt = conn.prepareStatement("SELECT * FROM CustomerSubscriptions WHERE customerId = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        if (!rs.next()) {
                buildResponse(outputStream, "Customer with id " + customerId + " does not have a subscription!", 200);
                return true;
        } else {
            subscriptionId = rs.getInt("subscriptionId");
            logger.log("Subscription id: " + subscriptionId + "\n");
            stmt2 = conn.prepareStatement("DELETE FROM SubscriptionServices WHERE subscriptionId = ?");
            stmt2.setInt(1, subscriptionId);
            stmt2.executeUpdate();
            stmt2.close();
            logger.log("Subscription id: " + subscriptionId + "\n");

            stmt2 = conn.prepareStatement("DELETE FROM CustomerSubscriptions WHERE customerId = ?");
            stmt2.setInt(1, customerId);
            stmt2.executeUpdate();
            stmt2.close();
            logger.log("Subscription id: " + subscriptionId + "\n");

            stmt2 = conn.prepareStatement("DELETE FROM Subscription WHERE id = ?");
            stmt2.setInt(1, subscriptionId);
            stmt2.executeUpdate();
            stmt2.close();
            buildResponse(outputStream, "Subscription canceled for customer with id " + customerId, 200);
            logger.log("Subscription id: " + subscriptionId + "\n");

        }


        rs.close();
        stmt.close();
        return false;
    }



    private JSONObject parseInput(InputStream inputStream, LambdaLogger logger) {
        JSONObject event = null;
        try {
            JSONParser parser = new JSONParser();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JSONObject msg = (JSONObject) parser.parse(reader);
            if (msg.get("body") == null) {
                throw new MissingFormatArgumentException("Missing body field");
            }
            logger.log("input:" + msg.toString());
            event = (JSONObject) parser.parse(msg.get("body").toString());
            logger.log(event.toString());

            if (event.get("customerId") == null) throw new MissingFormatArgumentException("No customerId defined!");
        } catch (Exception e) {
            logger.log(e.toString());
        }
        return event;
    }

    private void buildResponse(OutputStream outputStream, String responseMsg, int statusCode) throws IOException {
        JSONObject responseBody = new JSONObject();
        JSONObject responseJson = new JSONObject();
        JSONObject headerJson = new JSONObject();
        responseBody.put("message", responseMsg);
        responseJson.put("statusCode", statusCode);

        responseJson.put("headers", headerJson);
        responseJson.put("body", responseBody.toString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    private static void postMsg(JSONObject jsonObject, String contentType, String host, LambdaLogger logger) {
        try {
            HttpPost postRequest = new HttpPost(KONG_ENDPOINT);
            postRequest.addHeader("content-type", contentType);
            postRequest.addHeader("Host", host);
            postRequest.addHeader("apikey",API_KEY);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            StringEntity Entity = null;
            Entity = new StringEntity(jsonObject.toJSONString());
            postRequest.setEntity(Entity);
            HttpEntity base = postRequest.getEntity();
            HttpResponse response = null;
            response = httpClient.execute(postRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) logger.log("response body = " + EntityUtils.toString(responseEntity));
        } catch (Exception e) {
            logger.log(e.toString() + "\n");
        }
    }
}
