package ist.meic.ie.raisealarm;

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

public class RaiseAlarm implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);
        logger.log("1");
        if(obj.get("SIMCARD") == null) {
            LambdaUtils.buildResponse(outputStream, "No SIMCARD provided!", 500);
            return;
        }

        if(obj.get("type") == null) {
            LambdaUtils.buildResponse(outputStream, "No type provided!", 500);
            return;
        }

        int SIMCARD = ((Long) obj.get("SIMCARD")).intValue();
        String type = (String) obj.get("type");


        Connection conn = new DatabaseConfig(Constants.COMPANY_EVENTS_DB,"EventReader",Constants.COMPANY_EVENTS_DB_USER,Constants.COMPANY_EVENTS_PASSWORD).getConnection();

        PreparedStatement stmt;

        try {
            stmt = conn.prepareStatement("INSERT INTO Alarm (SIMCARD, type) VALUES (?,?)");
            stmt.setInt(1, SIMCARD);
            stmt.setString(2, type);
            stmt.executeUpdate();
            stmt.close();
            // GET CUSTOMER BY SIMCARD
            JSONObject simcardObj = new JSONObject();
            simcardObj.put("SIMCARD", SIMCARD);
            HttpResponse response = postMsg(simcardObj, "application/json", "getcustomerbysimcard.com", logger);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                LambdaUtils.buildResponse(outputStream, "Could not retrieve customer!", 500);
                return;
            }

            JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
            logger.log(responseObj.toJSONString());
            JSONObject message = (JSONObject) parser.parse((String) responseObj.get("message"));
            int customerId = Integer.parseInt((String)message.get("customerId"));

            JSONObject customerObj = new JSONObject();

            customerObj.put("customerId", customerId);
            logger.log("Customer Object");
            logger.log(customerObj.toJSONString());
            LambdaUtils.buildResponse(outputStream, customerObj.toJSONString(), 200);
        } catch (Exception e) {
            logger.log(e.toString());
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(e.toString());
            }
        }
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
