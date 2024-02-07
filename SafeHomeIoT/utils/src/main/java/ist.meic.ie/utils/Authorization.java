package ist.meic.ie.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Authorization {
    public static boolean checkUserPermission(String authorization, String user)
    {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            if(values[0].equals(user))
                return true;
            return false;
    }
}
