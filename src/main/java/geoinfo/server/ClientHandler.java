//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package geoinfo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler {
    public static void handleClient(Socket socket) {
        System.out.println("Server đã nhận kết nối từ Client: " + socket.getRemoteSocketAddress());
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        ) {
            String dataFromClient;
            while((dataFromClient = reader.readLine()) != null) {
                if (dataFromClient.trim().equalsIgnoreCase("bye")) {
                    writer.println("Kết thúc kết nối. Tạm biệt!");
                    break;
                }

                DataProcessor.processData(dataFromClient, writer);
                writer.println("<END>");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
