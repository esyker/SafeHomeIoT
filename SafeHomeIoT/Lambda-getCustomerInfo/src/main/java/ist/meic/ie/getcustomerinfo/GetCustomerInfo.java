package ist.meic.ie.getcustomerinfo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class GetCustomerInfo implements RequestStreamHandler {
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
        ResultSet rs, rs2;
        try {
            stmt = conn.prepareStatement("SELECT * FROM Customer WHERE id = ?");
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            if(!rs.next()) {
                LambdaUtils.buildResponse(outputStream, "Customer with id " + customerId + " does not exist!", 500);
                rs.close();
                stmt.close();
                return;
            }

            String firstName = rs.getString("firstName");
            String lastName = rs.getString("lastName");
            String street = rs.getString("street");
            String postalCode = rs.getString("postalCode");
            String district = rs.getString("district");
            String council = rs.getString("council");
            String parish = rs.getString("parish");
            String doorNumber = rs.getString("doorNumber");
            Date birthDate = rs.getDate("birthDate");



            rs.close();
            stmt.close();

            JSONArray devices = new JSONArray();
            stmt = conn.prepareStatement("SELECT SIMCARD, MSISDN, name FROM Device, DeviceType WHERE Device.deviceTypeId = DeviceType.id AND customerId = ?");
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            while(rs.next()) {
                JSONObject device = new JSONObject();
                device.put("SIMCARD", rs.getInt("SIMCARD"));
                device.put("MSISDN", rs.getInt("MSISDN"));
                device.put("deviceType", rs.getString("name"));
                devices.add(device);
            }
            rs.close();
            stmt.close();

            JSONArray services = new JSONArray();
            stmt = conn.prepareStatement("SELECT Service.name, Service.cost " +
                    "                           FROM Service, SubscriptionServices, Subscription, CustomerSubscriptions " +
                    "                          WHERE Service.id = SubscriptionServices.serviceId" +
                    "                               AND Subscription.id = SubscriptionServices.subscriptionId" +
                    "                               AND Subscription.id = CustomerSubscriptions.subscriptionId" +
                    "                               AND CustomerSubscriptions.customerId = ?");
            stmt.setInt(1, customerId);
            rs = stmt.executeQuery();
            while(rs.next()) {
                JSONObject service = new JSONObject();
                service.put("name", rs.getString("name"));
                service.put("cost", rs.getInt("cost"));
                services.add(service);
            }
            rs.close();
            stmt.close();

            JSONObject response = new JSONObject();
            response.put("firstName", firstName);
            response.put("lastName", lastName);
            response.put("street", street);
            response.put("postalCode", postalCode);
            response.put("district", district);
            response.put("council", council);
            response.put("parish", parish);
            response.put("doorNumber", doorNumber);
            response.put("birthDate", birthDate);
            response.put("devices", devices);
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
