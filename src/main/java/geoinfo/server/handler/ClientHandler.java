package geoinfo.server.handler;

import geoinfo.server.processor.DataProcessor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler {
    public static void handleClient(Socket socket) {
        System.out.println("Server da nhan ket noi tu Client UI: " + socket.getRemoteSocketAddress());

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String dataFromClient;
            while ((dataFromClient = reader.readLine()) != null) {
                if (dataFromClient.trim().equalsIgnoreCase("bye")) {
                    writer.println(new JSONObject()
                            .put("status", "success")
                            .put("message", "Ket thuc ket noi. Tam biet!")
                            .toString(2));
                    writer.println("<END>");
                    break;
                }

                String responseData = DataProcessor.processData(dataFromClient);
                writer.println(responseData);
                writer.println("<END>");
            }
        } catch (IOException e) {
            System.out.println("Mat ket noi voi Client: " + e.getMessage());
        }
    }
}
