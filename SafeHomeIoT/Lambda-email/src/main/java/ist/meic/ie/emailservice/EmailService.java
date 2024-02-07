package ist.meic.ie.emailservice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import ist.meic.ie.utils.LambdaUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EmailService implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        JSONParser parser = new JSONParser();
        JSONObject obj = LambdaUtils.parseInput(inputStream, logger);

        String email = (String) obj.get("email");
        String message = (String) obj.get("message");
        if (email == null) {
            LambdaUtils.buildResponse(outputStream, "No email provided!", 500);
            return;
        }
        if (message == null) {
            LambdaUtils.buildResponse(outputStream, "No email message provided!", 500);
            return;
        }

        SendEmail.send(email, "New Alarm!", message);
        LambdaUtils.buildResponse(outputStream, "Email sent!", 200);
    }
}