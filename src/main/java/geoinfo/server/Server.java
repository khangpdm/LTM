package geoinfo.server;

import geoinfo.server.handler.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try(ServerSocket server = new ServerSocket(port)){
            System.out.println("Server đang lắng nghe tại port " + port);
            while (true) {
                Socket socket = server.accept();
                ClientHandler.handleClient(socket);
            }
        } catch (IOException e) {
            System.out.println("Lỗi khởi tạo: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server(12345);
        server.start();
    }
}