package geoinfo.server.handler;

import geoinfo.common.SecurityConfig;
import geoinfo.common.SecurityUtils;
import geoinfo.server.processor.DataProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler {
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            // lay aes key, giai ma bang rsa private key cua server
            String encryptedKeyBase64 = reader.readLine();
            if (encryptedKeyBase64 == null) return;
            byte[] aesKey = SecurityUtils.decryptRSA(encryptedKeyBase64, SecurityConfig.getPrivateKey());

            String dataFromClient;
            while ((dataFromClient = reader.readLine()) != null) {
                // giai ma noi dung
                String decryptedInput = SecurityUtils.decryptAES(dataFromClient, aesKey);
                logInfo(socket, describeClientAction(decryptedInput));

                if (decryptedInput.trim().equalsIgnoreCase("bye")) {
                    writer.println(SecurityUtils.encryptAES("Goodbye!", aesKey));
                    writer.println("<END>");
                    break;
                }

                String responseData = DataProcessor.processData(decryptedInput);
                writer.println(SecurityUtils.encryptAES(responseData, aesKey));
                writer.println("<END>");
            }
        } catch (Exception e) {
            logError(socket, "Security error: " + e.getMessage());
        }
    }

    private static String describeClientAction(String rawInput) {
        if (rawInput == null) {
            return "Client gửi dữ liệu rỗng";
        }

        String normalizedInput = rawInput.trim();
        if (normalizedInput.isEmpty()) {
            return "Client gửi dữ liệu rỗng";
        }

        String lowerInput = normalizedInput.toLowerCase();
        if (lowerInput.equals("bye")) {
            return "Client kết thúc phiên làm việc";
        }
        if (lowerInput.equals("reloadcountry")) {
            return "Client yêu cầu tải lại dữ liệu quốc gia";
        }
        if (lowerInput.startsWith("country-more:")) {
            return "Client xem thêm thông tin quốc gia: " + extractKeyword(normalizedInput);
        }
        if (lowerInput.startsWith("city-more:")) {
            return "Client xem thêm thông tin thành phố: " + extractKeyword(normalizedInput);
        }
        if (lowerInput.startsWith("country:")) {
            return "Client tìm kiếm quốc gia: " + extractKeyword(normalizedInput);
        }
        if (lowerInput.startsWith("city:")) {
            return "Client tìm kiếm thành phố: " + extractKeyword(normalizedInput);
        }

        return "Client tìm kiếm: " + normalizedInput;
    }

    private static String extractKeyword(String request) {
        int delimiterIndex = request.indexOf(':');
        if (delimiterIndex < 0 || delimiterIndex == request.length() - 1) {
            return "(trống)";
        }
        String keyword = request.substring(delimiterIndex + 1).trim();
        return keyword.isEmpty() ? "(trống)" : keyword;
    }

    private static void logInfo(Socket socket, String message) {
        System.out.println(logPrefix("INFO", socket) + message);
    }

    private static void logError(Socket socket, String message) {
        System.err.println(logPrefix("ERROR", socket) + message);
    }

    private static String logPrefix(String level, Socket socket) {
        String clientAddress = "unknown";
        if (socket != null && socket.getInetAddress() != null) {
            clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return "[" + LocalDateTime.now().format(LOG_TIME_FORMAT) + "] [" + level + "] [CLIENT " + clientAddress + "] ";
    }
}
