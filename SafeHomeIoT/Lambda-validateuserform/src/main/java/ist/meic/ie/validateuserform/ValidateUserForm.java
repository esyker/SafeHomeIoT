package ist.meic.ie.validateuserform;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.sql.Connection;

public class ValidateUserForm implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);
        boolean valid = true;
        JSONObject resObj = new JSONObject();

        if (checkArgs(outputStream, obj)) return;
        String firstName = (String) obj.get("firstName");
        String lastName = (String) obj.get("lastName");
        String birthDate = (String) obj.get("birthDate");
        String street = (String) obj.get("street");
        String postalCode = (String) obj.get("postalCode");
        int doorNumber = ((Long) obj.get("doorNumber")).intValue();

        if (firstName.equals("") || lastName.equals("") || birthDate.equals("") || street.equals("") || postalCode.equals("") || doorNumber < 1) {
            resObj = new JSONObject();

            resObj.put("valid", false);
            LambdaUtils.buildResponse(outputStream, resObj.toJSONString(),500);
        }

        JSONObject validatedPostalCode = verifyPostalCode(postalCode, logger);
        if ((boolean) validatedPostalCode.get("valid") ) {
            validatedPostalCode.put("firstName", firstName);
            validatedPostalCode.put("lastName", lastName);
            validatedPostalCode.put("birthDate", birthDate);
            validatedPostalCode.put("street", street);
            validatedPostalCode.put("postalCode", postalCode);
        }
        System.out.println(validatedPostalCode.toJSONString());
        LambdaUtils.buildResponse(outputStream, validatedPostalCode.toJSONString(), 200);

    }

    private boolean checkArgs(OutputStream outputStream, JSONObject obj) throws IOException {
        if(obj.get("firstName") == null) {
            LambdaUtils.buildResponse(outputStream, "Fisrt name not provided", 500);
            return true;
        }

        if(obj.get("lastName") == null) {
            LambdaUtils.buildResponse(outputStream, "Last name not provided", 500);
            return true;
        }

        if(obj.get("birthDate") == null) {
            LambdaUtils.buildResponse(outputStream, "Birth date Id not provided", 500);
            return true;
        }

        if(obj.get("postalCode") == null) {
            LambdaUtils.buildResponse(outputStream, "Postal code not provided", 500);
            return true;
        }

        if(obj.get("street") == null) {
            LambdaUtils.buildResponse(outputStream, "Street not provided", 500);
            return true;
        }
        return false;
    }

    private JSONObject verifyPostalCode(String postalCode, LambdaLogger logger) {
        boolean ok = true;
        JSONObject validatedAddress = new JSONObject();

        try {
            String response = HTTPMessages.getXmlMsg("", postalCode);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document doc = builder.parse(is);

            String responseOK = doc.getDocumentElement().getNodeName();
            if (!responseOK.equals("OK")) {
                System.out.println("ERROR");
                ok = false;
            }

            NodeList districtNode = doc.getElementsByTagName("Distrito");
            NodeList councilNode = doc.getElementsByTagName("Concelho");
            NodeList parishNode = doc.getElementsByTagName("Freguesia");

            String district = districtNode.item(0).getTextContent();
            String council = councilNode.item(0).getTextContent();
            String parish = parishNode.item(0).getTextContent();
            System.out.println(district);
            System.out.println(council);
            System.out.println(parish);

            validatedAddress = new JSONObject();
            validatedAddress.put("valid", ok);
            validatedAddress.put("district", district);
            validatedAddress.put("council", council);
            validatedAddress.put("parish", parish);

            System.out.println(validatedAddress);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            logger.log(e.toString());
        }
        return validatedAddress;
    }
}
