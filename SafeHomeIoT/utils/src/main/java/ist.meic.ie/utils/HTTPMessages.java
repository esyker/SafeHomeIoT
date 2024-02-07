package ist.meic.ie.utils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;

import static ist.meic.ie.utils.Constants.*;

public class HTTPMessages {
    private static JSONParser jsonParser = new JSONParser();

    public static int postMsg(JSONObject jsonObject, String contentType, String host, LambdaLogger logger) {
        int statusCode = 0;
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
            statusCode = response.getStatusLine().getStatusCode();
            logger.log("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) logger.log("response body = " + EntityUtils.toString(responseEntity));
        } catch (Exception e) {
            logger.log(e.toString() + "\n");
        }
        return statusCode;
    }

    public static int postMsg(JSONObject jsonObject, String contentType, String host) {
        int statusCode = 0;
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
            statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) System.out.println("response body = " + EntityUtils.toString(responseEntity));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusCode;
    }

    public static JSONObject getMsg(JSONObject jsonObject, String contentType, String host, LambdaLogger logger) {
        int statusCode = 0;
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
            statusCode = response.getStatusLine().getStatusCode();
            if(statusCode!=200)
                return null;
            logger.log("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            JSONObject res =null;
            if (response != null) {
                res = (JSONObject) jsonParser.parse(EntityUtils.toString(response.getEntity()));
            }
            return res;
        } catch (Exception e) {
            logger.log(e.toString() + "\n");
        }
        return null;
    }

    public static JSONObject getMsg(JSONObject jsonObject, String contentType, String host) {
        int statusCode = 0;
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
            statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            if(statusCode!=200)
                return null;
            JSONObject res =null;
            if (response != null) {
                res = (JSONObject) jsonParser.parse(EntityUtils.toString(response.getEntity()));
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getXmlMsg(String contentType, String postalCode) {
        int statusCode = 0;
        String responseBody = "";
        try {
            HttpGet getRequest = new HttpGet(CTT_ENDPOINT + "?incodpos=" + postalCode);
            getRequest.addHeader("content-type", contentType);
            getRequest.addHeader("apikey",API_KEY);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            StringEntity Entity = null;
            //Entity = new StringEntity(jsonObject.toJSONString());
            HttpResponse response = null;
            response = httpClient.execute(getRequest);
            statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            HttpEntity responseEntity = response.getEntity();
            responseBody = EntityUtils.toString(responseEntity);
            if (responseEntity != null) System.out.println("response body = " + responseBody);
        } catch (Exception e) {
            System.out.println(e.toString() + "\n");
        }
        return responseBody;
    }
}
