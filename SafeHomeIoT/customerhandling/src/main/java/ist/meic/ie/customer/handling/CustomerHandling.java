package ist.meic.ie.customer.handling;

import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.camunda.bpm.client.ExternalTaskClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Logger;

import static ist.meic.ie.utils.Constants.API_KEY;
import static ist.meic.ie.utils.Constants.KONG_ENDPOINT;

public class CustomerHandling {
    private final static Logger LOGGER = Logger.getLogger(CustomerHandling.class.getName());
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Missing args: camunda endpoint");
            return;
        }

        String camundaEndpoint = args[0];
        validateDataForm(camundaEndpoint);
        createCustomer(camundaEndpoint);
        addIoTDevice(camundaEndpoint);
        subscribeToServices(camundaEndpoint);
    }

    private static void validateDataForm(String camundaEndpoint) {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl(camundaEndpoint + "/engine-rest")
                .asyncResponseTimeout(10) // polling timeout
                .build();
        client.subscribe("validate-customer-data-form")
                .lockDuration(10) // the default lock duration is 20 seconds, but you can override this
                .handler((externalTask, externalTaskService) -> {
                    boolean valid = true;
                    String firstName = (String) externalTask.getVariable("firstName");
                    String lastName = (String) externalTask.getVariable("lastName");
                    String address = (String) externalTask.getVariable("address");
                    Date rawBirthDate = ((Date) externalTask.getVariable("birthDate"));


                    if  (address.length() < 5) {
                        valid = false;
                    }

                    if (valid)
                        LOGGER.info("Form Validation succeeded!");
                    else
                        LOGGER.info("Form Validation failed!");

                    // Complete the task
                    externalTaskService.complete(externalTask, Collections.singletonMap("valid", valid));
                })
                .open();
    }

    private static void createCustomer(String camundaEndpoint) {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl(camundaEndpoint + "/engine-rest")
                .asyncResponseTimeout(10) // polling timeout
                .build();
        client.subscribe("create-new-user")
                .lockDuration(10) // the default lock duration is 20 seconds, but you can override this
                .handler((externalTask, externalTaskService) -> {
                    DatabaseConfig config = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling","pedro", "123456789");
                    String firstName = (String) externalTask.getVariable("firstName");
                    String lastName = (String) externalTask.getVariable("lastName");
                    String address = (String) externalTask.getVariable("address");
                    Date birthDate = ((Date) externalTask.getVariable("birthDate"));
                    try {
                        PreparedStatement insert = config.getConnection().prepareStatement ("INSERT INTO  Customer (firstname, lastname, address, birthdate) VALUES (?,?,?,?)");
                        insert.setString(1,firstName);
                        insert.setString(2,lastName);
                        insert.setString(3, address);
                        insert.setDate(4, new java.sql.Date(birthDate.getTime()));
                        insert.executeUpdate();
                        insert.close();
                        config.getConnection().close();
                        LOGGER.info("Inserted user: \n\tFirst Name: " + firstName + "\n\tLast Name: " + lastName + "\n\tAddress: " + address + "\n\tBirth Date: " + birthDate + "\n");
                        // CONVERT TO LAMBDA SERVICE AND ACTIVATE
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    externalTaskService.complete(externalTask);
                })
                .open();
    }

    static void addIoTDevice(String camundaEndpoint) {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl(camundaEndpoint + "/engine-rest")
                .asyncResponseTimeout(10) // polling timeout
                .build();
        client.subscribe("store-iot-device")
                .lockDuration(10) // the default lock duration is 20 seconds, but you can override this
                .handler((externalTask, externalTaskService) -> {
                    boolean valid = true;
                    if (externalTask.getVariable("userId") == null || externalTask.getVariable("SIMCARD") == null || externalTask.getVariable("MSISDN") == null) {
                        LOGGER.info("Missing parameters!");
                        externalTaskService.complete(externalTask);
                    }
                    int customerId = ((Long) externalTask.getVariable("userId")).intValue();
                    int SIMCARD = ((Long) externalTask.getVariable("SIMCARD")).intValue();
                    int MSISDN = ((Long) externalTask.getVariable("MSISDN")).intValue();
                    String deviceType = (String) externalTask.getVariable("deviceType");
                    PreparedStatement stmt;
                    ResultSet rs, rs2;
                    Connection conn = new DatabaseConfig(Constants.CUSTOMER_HANDLING_DB, "CustomerHandling","pedro", "123456789").getConnection();
                    try {
                        conn.setAutoCommit(false);
                        stmt = conn.prepareStatement ("select * from Customer WHERE id = ?");
                        stmt.setInt(1, customerId);
                        rs = stmt.executeQuery();
                        if(!rs.next()) {
                            LOGGER.info("User with userId " + customerId + " does not exist!");
                            conn.rollback();
                            return;
                        }
                        stmt.close();
                        stmt = conn.prepareStatement ("select * from Device WHERE SIMCARD = ?");
                        stmt.setInt(1, SIMCARD);
                        rs2 = stmt.executeQuery();
                        if (rs2.next()) {
                            LOGGER.info("Device with SIMCARD " + SIMCARD + " already exists!");
                            conn.rollback();
                            return;
                        }
                        stmt.close();


                        stmt = conn.prepareStatement ("select * from DeviceType WHERE name = ?");
                        stmt.setString(1, deviceType);
                        rs2 = stmt.executeQuery();
                        if (!rs2.next()) {
                            LOGGER.info("Device Type " + deviceType + " does not exist!");
                            conn.rollback();
                            return;
                        }
                        int deviceTypeId = rs2.getInt("id");
                        stmt.close();



                        // REMOTE CALL TO KONG EXPOSING PROVISION SERVICES
                        DefaultHttpClient httpClient = new DefaultHttpClient();
                        try
                        {
                            HttpPost postRequest = new HttpPost(KONG_ENDPOINT);
                            postRequest.addHeader("content-type", "application/json");
                            postRequest.addHeader("Host","activatesimcard.com");
                            JSONObject deviceJson = new JSONObject();
                            deviceJson.put("SIMCARD", SIMCARD);
                            deviceJson.put("MSISDN", MSISDN);
                            deviceJson.put("deviceType", deviceType);

                            LOGGER.info(postRequest.toString());
                            LOGGER.info(deviceJson.toJSONString());

                            StringEntity Entity = new StringEntity(deviceJson.toJSONString());
                            postRequest.setEntity(Entity);
                            HttpEntity base = postRequest.getEntity();
                            HttpResponse response = httpClient.execute(postRequest);
                            int statusCode = response.getStatusLine().getStatusCode();
                            LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
                            HttpEntity responseEntity = response.getEntity();
                            if(responseEntity!=null) LOGGER.info("response body = " + EntityUtils.toString(responseEntity));

                            // Insert Device
                            stmt = conn.prepareStatement("INSERT INTO Device (SIMCARD, MSISDN, customerId, deviceTypeId) VALUES (?,?,?,?)");
                            stmt.setInt(1, SIMCARD);
                            stmt.setInt(2, MSISDN);
                            stmt.setInt(3, customerId);
                            stmt.setInt(4, deviceTypeId);
                            stmt.executeUpdate();
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally { httpClient.getConnectionManager().shutdown(); }

                        conn.commit();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        try {
                            conn.rollback();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    // Complete the task
                    externalTaskService.complete(externalTask);
                })
                .open();
    }

    private static void subscribeToServices(String camundaEndpoint) {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl(camundaEndpoint + "/engine-rest")
                .asyncResponseTimeout(10) // polling timeout
                .build();
        client.subscribe("subscribe-to-services")
                .lockDuration(10) // the default lock duration is 20 seconds, but you can override this
                .handler((externalTask, externalTaskService) -> {
                    int customerId = ((Long) externalTask.getVariable("customerId")).intValue();
                    boolean homeSecurity = (boolean) externalTask.getVariable("homeSecurity");
                    boolean homeInventoryManagement = (boolean) externalTask.getVariable("homeInventoryManagement");
                    LOGGER.info("customerId: " + customerId + "\n");
                    LOGGER.info("homeSecurity: " + homeSecurity + "\n");
                    LOGGER.info("homeInventoryManagement: " + homeInventoryManagement + "\n");

                    JSONObject services = new JSONObject();
                    JSONArray listServices = new JSONArray();
                    if (homeSecurity) listServices.add(1);
                    if (homeInventoryManagement) listServices.add(2);

                    services.put("customerId", customerId);
                    services.put("services", listServices);
                    LOGGER.info(services.toJSONString());
                    postMsg(services, "application/json", "subscribetoservices.com");
                    externalTaskService.complete(externalTask);
                })
                .open();
    }

    private static void postMsg(JSONObject jsonObject, String contentType, String host) {
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
            LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) LOGGER.info("response body = " + EntityUtils.toString(responseEntity));
        } catch (Exception e) {
            LOGGER.info(e.toString() + "\n");
        }
    }
}
