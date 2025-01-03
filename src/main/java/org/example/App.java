package org.example;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private static final Logger LOGGER = Logger.getLogger(App.class);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10); // Sử dụng pool với 10 luồng

    public static void main( String[] args )
    {
        if (args.length > 0) {
            try {
                fileCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid number of arguments: " + args[0]);
                return;
            }
        }

        try {
            while (true) {
//                1. Tạo nội dung file với UUID v4 và timestamp hiện tại
                String uuid = UUID.randomUUID().toString();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
                String fileContent = uuid + " ---> " + timestamp;

//                2. Tạo mã hash (SHA-256) từ nội dung file
                String hash = generateHash(fileContent);

//                3. Tạo file với tên là mã hash và nội dung trên
                String fileName = uuid + ".txt";
                File file = new File(fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(fileContent.getBytes(StandardCharsets.UTF_8));
                }

                System.out.println();
                LOGGER.info("File created: " + fileName);

//                4. Tạo một luồng con để upload và xóa file
                executorService.submit(() -> {
                   try {
                       if (uploadFile(file)) {
                            synchronized (App.class) {
                                fileCount++;
                                LOGGER.info("File uploaded successfully: " + fileName);
                                LOGGER.info("Total files uploaded: " + fileCount);
                            }

//                            Xóa file sau khi upload
                           if (file.delete()) {
                               LOGGER.info("File deleted locally: " + fileName);
                           } else {
                               LOGGER.error("Failed to delete file: " + fileName);
                           }
                       } else {
                            LOGGER.error("Failed to upload file: " + fileName);
                        }
                   } catch (Exception e) {
                       LOGGER.error(e);
                   }
                });

//                Tạm dừng giữa các lần thực hiện để tránh quá tải (tùy chỉnh nếu cần)
                Thread.sleep(500);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private static boolean uploadFile(File file) throws IOException, InterruptedException {
        String apiUrl = "https://dev-fms.mobile-id.vn/FMS/app_test/upload";
        String[] command = {
                "curl", "-X", "POST",
                apiUrl,
                "-H", "Content-type:application/octet-stream",
                "-H", "x-format:txt",
                "--data-binary", "@" + file.getAbsolutePath()
        };

        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return true;
        } else {
            LOGGER.error("File upload failed. Exit code: " + exitCode);
            return false;
        }
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
}
