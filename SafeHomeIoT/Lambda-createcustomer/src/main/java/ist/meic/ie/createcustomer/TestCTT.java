package ist.meic.ie.createcustomer;

import ist.meic.ie.utils.HTTPMessages;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class TestCTT {


    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        boolean ok = true;
        try {
            String response = HTTPMessages.getXmlMsg("", "2435-477");
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

            JSONObject validatedAddress = new JSONObject();
            validatedAddress.put("valid", ok);
            validatedAddress.put("district", district);
            validatedAddress.put("council", council);
            validatedAddress.put("parish", parish);

            System.out.println(validatedAddress);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }
}
