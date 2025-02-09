package org.example;

import Utils.Utils;
import org.apache.log4j.Logger;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 *
 */
public class App 
{
    private static int fileCount = 0;
    private static int clientCount = 0;
    private static int SumCount = 0;
    private static int counter = 0;
    private static final Logger LOGGER = Logger.getLogger(App.class);
    private static final ExecutorService executorService1 = Executors.newSingleThreadExecutor();
    private static final ExecutorService executorService2 = Executors.newSingleThreadExecutor();
    private static final ExecutorService executorService3 = Executors.newSingleThreadExecutor();
    private static final ExecutorService executorService4 = Executors.newSingleThreadExecutor();
    private static final ExecutorService executorService5 = Executors.newSingleThreadExecutor();

    private static String FROM_EMAIL; //requires valid gmail id
    private static String PASSWORD; // correct password for gmail id
    private static String TO_EMAIL; // can be any email id
    private static String SMTP_HOST;
    private static String TLS_PORT;
    private static String ENABLE_AUTHENTICATION;
    private static String ENABLE_STARTTLS;
    private static String PATH_SEND_EMAIL_CONFIG = "";

    private static long startTime;

    public static void main( String[] args )
    {
        if (args.length > 0) {
            PATH_SEND_EMAIL_CONFIG = args[0];
        } else {
            LOGGER.error("Invalid number 1 of arguments: ");
        }

        if (args.length > 1) {
            try {
                clientCount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid number 2 of arguments: " + args[1]);
                return;
            }
        }

        if (args.length > 2) {
            try {
                fileCount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid number 3 of arguments: " + args[2]);
                return;
            }
        }

        if (args.length > 3) {
            try {
                SumCount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid number 4 of arguments: " + args[3]);
                return;
            }
        }

        startTime = System.currentTimeMillis();
        counter = 0;

//        CLIENT 01
//        genClient(executorService1, "01");
//        genClient(executorService2, "02");
//        genClient(executorService3, "03");
//        genClient(executorService4, "04");
//        genClient(executorService5, "05");

        for (int i = 0; i < clientCount; i++) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            genClient(executorService, String.valueOf(i+1));
        }
    }

    private static void genClient( ExecutorService executorService, String clientID) {
        LOGGER.info("[CLIENT-ID: " + clientID + "] | [INITIALIZATION] initialized successfully.");
        try {
            executorService.submit(() -> {
                while (SumCount == 0 || fileCount < SumCount) {
                    try {
//                1. Tạo nội dung file với UUID v4 và timestamp hiện tại
                        String uuid = UUID.randomUUID().toString();
                        String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                        String fileContent = uuid + " ---> " + timestamp;

//                2. Tạo mã hash (SHA-256) từ nội dung file
                        String hash = generateHash(fileContent);

//                3. Tạo file với tên là mã hash và nội dung trên
                        String fileName = hash + ".txt";
                        File file = new File(fileName);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(fileContent.getBytes(StandardCharsets.UTF_8));
                        }
                        LOGGER.info("[CLIENT-ID: " + clientID + "] | [CREATE] File created: " + fileName);
                        LOGGER.info("[CLIENT-ID: " + clientID + "] | [CONTENT] Content of file: " + fileContent);

//                4.Upload và xóa file
                        if (uploadFile(file, clientID, executorService)) {
                            synchronized (App.class) {
                                fileCount++;
                                counter++;
                            }

//                            Xóa file sau khi upload
                            if (file.delete()) {
                                LOGGER.info("[CLIENT-ID: " + clientID + "] | [DELETED FILE SUCCESSFULLY] File deleted locally: " + fileName);
                            } else {
                                LOGGER.error("[CLIENT-ID: " + clientID + "] | [DELETED FILE FAILURE] Failed to delete file: " + fileName);
                            }

//                            Tính tổng thời gian thực thi (ms -> s)
                            long elapsedTimeInMillis = System.currentTimeMillis() - startTime;
                            double elapsedTimeInSeconds = elapsedTimeInMillis / 1000.0;
//                            Tính TPS
                            double TPS = counter / elapsedTimeInSeconds;

                            LOGGER.info("[CLIENT-ID: " + clientID + "] | [TOTAL] Total files uploaded: " + fileCount );
                            LOGGER.info("[CLIENT-ID: " + clientID + "] | [TPS] tps: " + TPS + " | counter: " + counter + " | elapsed time in seconds: " + elapsedTimeInSeconds);
                        }
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }

                    if (fileCount >= SumCount) {
                        LOGGER.info("[CLIENT-ID: " + clientID + "] | [STOP TOOL] " + fileCount + " files uploaded");

                        String formattedDateTime = getDayTime();
                        String subject = "STOP TOOL HANDLE FILES FMS";
                        String body = "TOOL STOP INFORMATION: \n" +
                                "Code: NULL\n" +
                                "Status: SUCCESS\n" +
                                "Message: " + fileCount + " files uploaded" + "\n" +
                                "Client-ID: " + clientID + "\n" +
                                "Function: genClient (try)\n" +
                                "Day: " + formattedDateTime;

                        sendEmail(subject, body);

                        executorService.shutdown();
                        System.exit(0);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error(e);

            String formattedDateTime = getDayTime();
            String subject = "[ERROR] STOP TOOL HANDLE FILES FMS";
            String body = "TOOL STOP INFORMATION: \n" +
                    "Code: NULL\n" +
                    "Status: ERROR\n" +
                    "Message: " + e.getMessage() + "\n" +
                    "Client-ID: " + clientID + "\n" +
                    "Function: genClient (catch)\n" +
                    "Day: " + formattedDateTime;

            sendEmail(subject, body);

            executorService.shutdown();
        }
    }
    
    private static boolean uploadFile(File file, String clientID, ExecutorService executorService) throws IOException, InterruptedException {
        String apiUrl = "https://dev-fms.mobile-id.vn/FMS/app_test/upload";
        String[] command = {
                "curl", "-X", "POST",
                apiUrl,
                "-H", "Content-type:application/octet-stream",
                "-H", "x-format:txt",
                "--max-time", "10", // Thời gian chờ tối đa là 10 giây
//                "-H", "x-temp:true",
                "--data-binary", "@" + file.getAbsolutePath()
        };

        synchronized (App.class) {
            if (fileCount >= SumCount) {
                LOGGER.info("[CLIENT-ID: " + clientID + "] | [STOP TOOL] " + fileCount + " files uploaded");

                String formattedDateTime = getDayTime();
                String subject = "STOP TOOL HANDLE FILES FMS";
                String body = "TOOL STOP INFORMATION: \n" +
                        "Code: NULL\n" +
                        "Status: SUCCESS\n" +
                        "Message: " + fileCount + " files uploaded" + "\n" +
                        "Client-ID: " + clientID + "\n" +
                        "Function: uploadFile\n" +
                        "Day: " + formattedDateTime;

                sendEmail(subject, body);

                executorService.shutdown();
                System.exit(0);
            }
        }

        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        // Đọc phản hồi từ API
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Bỏ qua dòng trắng
                    response.append(line).append(System.lineSeparator());
                }
            }

            if (exitCode == 0) {
                LOGGER.info("[CLIENT-ID: " + clientID + "] | [UPLOAD] File uploaded successfully. fileName: " + file.getName() + " | Response: " + response);
                return true;
            } else {
                LOGGER.error("[CLIENT-ID: " + clientID + "] | [UPLOAD] File upload failed. fileName: " + file.getName() + " | Exit code: " + exitCode + " | Response: " + response);
                return uploadWithRetry(file, 3, clientID, executorService);
//                return false;
            }
        } catch (Exception e) {
            LOGGER.error(e);

            String formattedDateTime = getDayTime();
            String subject = "[ERROR] STOP TOOL HANDLE FILES FMS";
            String body = "TOOL STOP INFORMATION: \n" +
                    "Code: NULL\n" +
                    "Status: ERROR\n" +
                    "Message: " + e.getMessage() + "\n" +
                    "Client-ID: " + clientID + "\n" +
                    "Function: uploadFile (catch)\n" +
                    "Day: " + formattedDateTime;

            sendEmail(subject, body);
            return false;
        } finally {
            process.destroy();
        }
    }

    //    HttpURLConnection cho phép kiểm soát trực tiếp luồng dữ liệu và tránh phụ thuộc vào Process
    private static boolean uploadFileUsingHttp(File file, String clientID, ExecutorService executorService) {
        String apiUrl = "https://dev-fms.mobile-id.vn/FMS/app_test/upload";
        try {
            // Thiết lập kết nối
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("x-format", "txt");

            // Thiết lập thời gian chờ
            connection.setConnectTimeout(10000); // 10 giây cho việc kết nối
            connection.setReadTimeout(10000);    // 10 giây cho việc đọc dữ liệu

            // Ghi dữ liệu file vào OutputStream của kết nối
            try (OutputStream os = connection.getOutputStream();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            // Lấy mã phản hồi từ server
            int responseCode = connection.getResponseCode();

            // Đọc phản hồi từ InputStream
            InputStream is = (responseCode == HttpURLConnection.HTTP_OK)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append(System.lineSeparator());
                }
            }

            // Xử lý phản hồi
            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOGGER.info("[UPLOADED SUCCESSFULLY] File uploaded: " + file.getName() + " | Response: " + response);
                return true;
            } else {
                LOGGER.error("[UPLOADED FAILURE] HTTP response code: " + responseCode + " | Response: " + response);
                return uploadWithRetry(file, 3, clientID, executorService);
//                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error uploading file", e);
            return false;
        }
    }

    private static boolean uploadWithRetry(File file, int retries, String clientID, ExecutorService executorService) throws InterruptedException, IOException {
        for (int i = 0; i < retries; i++) {
            if (uploadFile(file, clientID, executorService)) {
                return true;
            }
            Thread.sleep(500); // Tạm dừng 1 giây trước khi thử lại
        }
        return false;
    }

    private static String generateHash(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static void sendEmail(String subject, String body) {
        System.out.println();
        getSendEmailConfig(PATH_SEND_EMAIL_CONFIG); // Read file config to send email
        System.out.println("TLSEmail Start");
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST); //SMTP Host
        props.put("mail.smtp.port", TLS_PORT); //TLS Port
        props.put("mail.smtp.auth", ENABLE_AUTHENTICATION); //enable authentication
        props.put("mail.smtp.starttls.enable", ENABLE_STARTTLS); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        };
        Session session = Session.getInstance(props, auth);

        Utils.sendEmail(session, TO_EMAIL,subject, body);
    }

    private static void getSendEmailConfig(String pathConfig) {
        TreeMap<String, Object> map = Utils.readSendEmailConfig(pathConfig);
        if (map != null) {
            for (String key : map.keySet()) {
                switch (key) {
                    case "FROM_EMAIL":
                        FROM_EMAIL = String.valueOf(map.get(key));
                        break;

                    case "PASSWORD":
                        PASSWORD = String.valueOf(map.get(key));
                        break;

                    case "TO_EMAIL":
                        TO_EMAIL = String.valueOf(map.get(key));
                        break;

                    case "SMTP_HOST":
                        SMTP_HOST = String.valueOf(map.get(key));
                        break;

                    case "TLS_PORT":
                        TLS_PORT = String.valueOf(map.get(key));
                        break;

                    case "ENABLE_AUTHENTICATION":
                        ENABLE_AUTHENTICATION = String.valueOf(map.get(key));
                        break;

                    case "ENABLE_STARTTLS":
                        ENABLE_STARTTLS = String.valueOf(map.get(key));
                        break;
                }
            }

            if (FROM_EMAIL == null || FROM_EMAIL.isEmpty() || PASSWORD == null || PASSWORD.isEmpty() ||
                    TO_EMAIL == null || TO_EMAIL.isEmpty() || SMTP_HOST == null || SMTP_HOST.isEmpty() ||
                    TLS_PORT == null || TLS_PORT.isEmpty() || ENABLE_AUTHENTICATION == null || ENABLE_AUTHENTICATION.isEmpty() ||
                    ENABLE_STARTTLS == null || ENABLE_STARTTLS.isEmpty()) {
                System.out.println("Invalid configuration parameter");
                System.exit(0);
            } else
                System.out.println("Configuration send Email parameters loaded successfully");
        }
    }

    private static String getDayTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
