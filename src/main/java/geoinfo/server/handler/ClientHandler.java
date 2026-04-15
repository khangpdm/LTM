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

public class ClientHandler {
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
            System.out.println("Security Error: " + e.getMessage());
        }
    }
}
