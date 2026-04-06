package geoinfo.server;

import geoinfo.server.processor.DataProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server {
    private final int port;
    private final int bufferSize;

    public Server(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Server đang chạy ở port: " + socket.getLocalPort());
            while (true) {
                DatagramPacket packet = receiveData(socket);
                String receivedData = new String(
                        Arrays.copyOf(packet.getData(), packet.getLength()),
                        StandardCharsets.UTF_8
                );
                System.out.println("Server nhận: " + receivedData);
                if (receivedData.equalsIgnoreCase("exit")) {
                    System.out.println("Server đang đóng kết nối.");
                    break;
                }
                sendData(socket, packet, receivedData);
            }
        } catch (IOException e) {
            System.out.println("Lỗi khởi tạo server: " + e.getMessage());
        }
    }

    private DatagramPacket receiveData(DatagramSocket socket) throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
        socket.receive(receivePacket);
        System.out.println(
                "Nhận yêu cầu từ client "
                        + receivePacket.getAddress().getHostAddress()
                        + ":"
                        + receivePacket.getPort()
        );
        return receivePacket;
    }

    private void sendData(DatagramSocket socket, DatagramPacket receivePacket, String receivedData) throws IOException {
        String response = DataProcessor.processData(receivedData);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
                responseBytes,
                responseBytes.length,
                receivePacket.getAddress(),
                receivePacket.getPort()
        );
        socket.send(packet);
    }

    public static void main(String[] args) {
        Server server = new Server(1234, 16384);
        server.start();
    }
}
