package geoinfo.client;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class Client{
    private String host;
    private int port;

    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void start(){
        try(Socket socket = new Socket(host,port)){
            System.out.println("Đã kết nối đến Server " + socket.getRemoteSocketAddress());
            startCommunication(socket);
        } catch(IOException e){
            System.out.println("Lỗi kết nối " + e.getMessage());
        }
    }

    private void startCommunication(Socket socket){
       try(Scanner scanner = new Scanner(System.in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)){
           String userinput;
           while(true){
               System.out.print("Mời bạn nhập dữ liệu: ");
               userinput = scanner.nextLine();
               writer.println(userinput);
               String response;
               while((response = reader.readLine()) != null){
                   if(response.equals("<END>")){
                       break;
                   }
                   System.out.println(response);
               }
               if(userinput.equalsIgnoreCase("bye")){
                   System.out.println("Client gửi yêu cầu đóng kết nối.");
                   break;
               }
           }
       } catch(IOException e){
            System.out.println("Lỗi gửi/nhận dữ liệu " + e.getMessage());
       }
    }

    public static void main(String[] args){
        Client client = new Client("localhost",12345);
        client.start();
    }
}
