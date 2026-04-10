package geoinfo.server.handler;

import geoinfo.server.processor.DataProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler {
    public static void handleClient(Socket socket) {
        System.out.println("Server đã nhận kết nối từ Client UI: " + socket.getRemoteSocketAddress());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            String dataFromClient;
            while((dataFromClient = reader.readLine()) != null) {
                if (dataFromClient.trim().equalsIgnoreCase("bye")) {
                    writer.println("Kết thúc kết nối. Tạm biệt!");
                    writer.println("<END>");
                    break;
                }

                // 1. Chuyển request cho DataProcessor xử lý và lấy kết quả
                String responseData = DataProcessor.processData(dataFromClient);

                // 2. Gửi kết quả về cho TextArea của UI
                writer.println(responseData);

                // 3. Gửi cờ hiệu <END> để báo cho UI biết Server đã gửi xong kết quả
                writer.println("<END>");
            }

        } catch (IOException e) {
            System.out.println("Mất kết nối với Client: " + e.getMessage());
        }
    }
}

//import geoinfo.server.processor.DataProcessor;

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.Socket;
//
//public class ClientHandler {
//    public static void handleClient(Socket socket) {
//        System.out.println("Server đã nhận kết nối từ Client: " + socket.getRemoteSocketAddress());
//        try (
//                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
//        ) {
//            String dataFromClient;
//            while((dataFromClient = reader.readLine()) != null) {
//                if (dataFromClient.trim().equalsIgnoreCase("bye")) {
//                    writer.println("Kết thúc kết nối. Tạm biệt!");
//                    break;
//                }
//
//                DataProcessor.processData(dataFromClient, writer);
//                writer.println("<END>");
//            }
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//
//    }
//}