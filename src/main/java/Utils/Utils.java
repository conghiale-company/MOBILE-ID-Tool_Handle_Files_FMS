package Utils;

/**
 * Project: TOOL_HANDLE_FILES_FMS
 * Created by Cong Nghia le
 * Date: 2025/01/08
 * Time: 11:48 AM
 */

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.TreeMap;

/**
 * @ 2025 Conghiale. All rights reserved
 */

public class Utils {
    public static void sendEmail(Session session, String toEmail, String subject, String body){
        try
        {
            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress("nghialc@mobile-id.vn", "Conghiale"));
            msg.setReplyTo(InternetAddress.parse("nghialc@mobile-id.vn", false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            System.out.println("Message is ready");
            Transport.send(msg);

            System.out.println("EMail Sent Successfully!!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TreeMap<String, Object> readSendEmailConfig(String path) {
        TreeMap<String, Object> map = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("FROM_EMAIL")) {
                    map.put("FROM_EMAIL", line.split(" = ")[1]);
                } else if (line.contains("PASSWORD")) {
                    map.put("PASSWORD", line.split(" = ")[1]);
                } else if (line.contains("TO_EMAIL")) {
                    map.put("TO_EMAIL", line.split(" = ")[1]);
                } else if (line.contains("SMTP_HOST")) {
                    map.put("SMTP_HOST", line.split(" = ")[1]);
                } else if (line.contains("TLS_PORT")) {
                    map.put("TLS_PORT", line.split(" = ")[1]);
                } else if (line.contains("ENABLE_AUTHENTICATION")) {
                    map.put("ENABLE_AUTHENTICATION", line.split(" = ")[1]);
                } else if (line.contains("ENABLE_STARTTLS")) {
                    map.put("ENABLE_STARTTLS", line.split(" = ")[1]);
                }
            }

            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
