package ist.meic.ie.getdeviceinfo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static ist.meic.ie.utils.Constants.API_KEY;
import static ist.meic.ie.utils.Constants.KONG_ENDPOINT;

public class GetDeviceInfo implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);

        if(obj.get("customerId") == null) {
            LambdaUtils.buildResponse(outputStream, "Customer Id not provided", 500);
            return;
        }

        if(obj.get("SIMCARD") == null) {
            LambdaUtils.buildResponse(outputStream, "SIMCARD not provided", 500);
            return;
        }

        int customerId = ((Long) obj.get("customerId")).intValue();
        int SIMCARD = ((Long) obj.get("SIMCARD")).intValue();

        Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling",Constants.CUSTOMER_HANDLING_DB_USER, Constants.CUSTOMER_HANDLING_DB_PASSWORD).getConnection();
        PreparedStatement stmt;
        ResultSet rs;
        try {
            if (checkCustomerId(outputStream, customerId, conn)) return;

            stmt = conn.prepareStatement("SELECT * FROM Device WHERE SIMCARD = ? AND customerId = ?");
            stmt.setInt(1, SIMCARD);
            stmt.setInt(2, customerId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                LambdaUtils.buildResponse(outputStream, "Customer " + customerId + " has no device with SIMCARD " + SIMCARD , 500);
                return;
            }
            int MSISDN = rs.getInt("MSISDN");
            int deviceTypeId = rs.getInt("deviceTypeId");
            rs.close();
            stmt.close();

            String deviceType = "";
            stmt = conn.prepareStatement("SELECT * FROM DeviceType WHERE id = ?");
            stmt.setInt(1, deviceTypeId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                deviceType = rs.getString("name");
            }

            JSONObject simcardObj = new JSONObject();
            simcardObj.put("SIMCARD", SIMCARD);
            HttpResponse response = postMsg(simcardObj, "application/json", "getsimcardstatus.com", logger);
            logger.log("1");
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log("2");
            if (statusCode != 200) {
                LambdaUtils.buildResponse(outputStream, "Could not retrieve simcard status!", 500);
                return;
            }
            JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
            logger.log(responseObj.toJSONString());
            String simcardStatus = (String) responseObj.get("message");

            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("SIMCARD", SIMCARD);
            deviceInfo.put("MSISDN", MSISDN);
            deviceInfo.put("deviceType", deviceType);
            deviceInfo.put("status", simcardStatus);

            LambdaUtils.buildResponse(outputStream, deviceInfo.toJSONString(), 200);

        } catch (Exception e) {
            logger.log(e.toString());
        }

    }

    private boolean checkCustomerId(OutputStream outputStream, int customerId, Connection conn) throws SQLException, IOException {
        PreparedStatement stmt;
        ResultSet rs;
        stmt = conn.prepareStatement("SELECT * FROM Customer WHERE id = ?");
        stmt.setInt(1, customerId);
        rs = stmt.executeQuery();
        if (!rs.next()) {
            LambdaUtils.buildResponse(outputStream, "Customer with id " + customerId + " not found!", 500);
            return true;
        }
        rs.close();
        stmt.close();
        return false;
    }

    private static HttpResponse postMsg(JSONObject jsonObject, String contentType, String host, LambdaLogger logger) throws IOException, ParseException {
        int statusCode = 0;
        JSONParser jsonParser = new JSONParser();
        HttpEntity responseEntity = null;
        HttpResponse response = null;

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
            response = httpClient.execute(postRequest);
            statusCode = response.getStatusLine().getStatusCode();
            logger.log("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            //responseEntity = response.getEntity();
            //if (responseEntity != null) logger.log("response body = " + EntityUtils.toString(responseEntity));
        } catch (Exception e) {
            logger.log(e.toString() + "\n");
        }
        return response;
    }
}
