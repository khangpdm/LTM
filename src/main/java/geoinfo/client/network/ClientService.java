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
    private final String host;
    private final int port;

    public ClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean connect() {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public String sendRequest(String message) {
        try (
                Socket socket = new Socket(host, port);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            socket.setSoTimeout(100000);
            writer.println(message);
            writer.flush();

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                response.append(line).append("\n");
            }
            return response.toString();
        } catch (SocketTimeoutException e) {
            return "Loi: Server khong phan hoi (timeout)";
        } catch (IOException e) {
            return "Loi: " + e.getMessage();
        }
    }

    public void disconnect() {
        // Each request uses its own socket, so there is no persistent connection to close.
    }
}
