package ist.meic.ie.emailservice;

import ist.meic.ie.utils.Constants;

import java.io.IOException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class SendEmail {

    public static void send(String to, String subject, String msg ) throws IOException {
        final String from = Constants.ENTERPRISE_EMAIL; // from address. As this is using Gmail SMTP.
        final String password = Constants.ENTERPRISE_EMAIL_PASSWORD; // password for from mail address.
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
            Transport.send(message);
            System.out.println("Mail successfully sent..");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}

