package geoinfo.client.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class ClientService {
    private final String host;
    private final int port;
    private final int bufferSize;

    public ClientService(String host, int port, int bufferSize) {
        this.host = host;
        this.port = port;
        this.bufferSize = bufferSize;
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {
            socket.setSoTimeout(30000);
            InetAddress address = InetAddress.getByName(host);

            while (true) {
                String input = getInput(scanner);
                sendData(socket, address, input);

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Client nhận được yêu cầu kết thúc.");
                    break;
                }

                String receivedData = receiveData(socket);
                System.out.println("Client nhận:\n" + receivedData);
            }
        } catch (IOException e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
        }
    }

    private String getInput(Scanner scanner) {
        System.out.print("Nhập dữ liệu: ");
        return scanner.nextLine();
    }

    private void sendData(DatagramSocket socket, InetAddress address, String input) throws IOException {
        byte[] dataBytes = input.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length, address, port);
        socket.send(packet);
    }

    private String receiveData(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
        socket.receive(packet);
        byte[] dataBytes = Arrays.copyOf(packet.getData(), packet.getLength());
        return new String(dataBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        ClientService client = new ClientService("127.0.0.1", 1234, 16384);
        client.start();
    }
}
