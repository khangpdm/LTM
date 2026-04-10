package geoinfo.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;


public class ClientService {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            return true;
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public String sendRequest(String message) {
        if (socket == null || socket.isClosed()) {
            return "Chưa kết nối đến server!";
        }

        try {
            socket.setSoTimeout(10000);
            writer.println(message);
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                response.append(line).append("\n");
            }
            return response.toString();
        }catch (SocketTimeoutException e) {
            return "Lỗi: Server không phản hồi (timeout)";
        } catch (IOException e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    public void disconnect() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }
}
