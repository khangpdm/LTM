package geoinfo.client.network;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

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
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
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
            writer.println(message);
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) break;
                response.append(line).append("\n");
            }
            return response.toString();

        } catch (IOException e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && ! socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }
}

//public class ClientService {
//    private String host;
//    private int port;
//
//    public void start(){
//        try(Socket socket = new Socket(host,port)){
//            System.out.println("Đã kết nối đến Server " + socket.getRemoteSocketAddress());
//            startCommunication(socket);
//        } catch(IOException e){
//            System.out.println("Lỗi kết nối " + e.getMessage());
//        }
//    }
//
//
//    private void startCommunication(Socket socket){
//       try(Scanner scanner = new Scanner(System.in);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)){
//           String userinput;
//           while(true){
//               System.out.print("Mời bạn nhập dữ liệu: ");
//               userinput = scanner.nextLine();
//               writer.println(userinput);
//               String response;
//               while((response = reader.readLine()) != null){
//                   if(response.equals("<END>")){
//                       break;
//                   }
//                   System.out.println(response);
//               }
//               if(userinput.equalsIgnoreCase("bye")){
//                   System.out.println("Client gửi yêu cầu đóng kết nối.");
//                   break;
//               }
//           }
//       } catch(IOException e){
//            System.out.println("Lỗi gửi/nhận dữ liệu " + e.getMessage());
//       }
//    }
//}
