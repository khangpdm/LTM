package geoinfo.server;

import geoinfo.server.handler.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private int port;
    private ThreadPoolExecutor threadPool;
    private volatile boolean isRunning;
    private ServerSocket serverSocket;

    // Constructor với các tham số tùy chỉnh
    public Server(int port, int corePoolSize, int maxPoolSize, int queueCapacity) {
        this.port = port;
        this.isRunning = true;

        // Tạo ThreadPoolExecutor với đầy đủ tham số
        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,           // Số thread tối thiểu luôn duy trì
                maxPoolSize,            // Số thread tối đa
                60L,                    // Thời gian giữ thread rảnh
                TimeUnit.SECONDS,       // Đơn vị thời gian
                new ArrayBlockingQueue<>(queueCapacity),  // Hàng đợi chờ
                new CustomThreadFactory(),               // Tạo thread với tên tùy chỉnh
                new CustomRejectedExecutionHandler()     // Xử lý khi hàng đợi đầy
        );

        // Cho phép core threads timeout nếu không có việc
        threadPool.allowCoreThreadTimeOut(true);
    }

    // Constructor mặc định
    public Server(int port) {
        this(port, 10, 50, 100);  // core=10, max=50, queue=100
    }


    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("||=========================================================||");
            System.out.printf ("|| %-55s ||\n", "The Server is listening at the port: " + port);
            System.out.println("||=========================================================||");
            System.out.printf ("|| %-55s ||\n", "  - Core Pool Size: " + threadPool.getCorePoolSize());
            System.out.printf ("|| %-55s ||\n", "  - Maximum Pool Size: " + threadPool.getMaximumPoolSize());
            System.out.printf ("|| %-55s ||\n", "  - Queue Capacity: " + threadPool.getQueue().remainingCapacity());
            System.out.printf ("|| %-55s ||\n", "  - Keep Alive Time: 60 seconds");
            System.out.println("||=========================================================||\n");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[+] Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    // Hiển thị trạng thái pool trước khi xử lý
                    printPoolStatus();
                    // Gửi task vào ThreadPoolExecutor
                    threadPool.execute(() -> {
                        try {
                            ClientHandler.handleClient(clientSocket);
                        } catch (Exception e) {
                            System.err.println("Lỗi xử lý client: " + e.getMessage());
                        } finally {
                            try {
                                clientSocket.close();
                                System.out.println("[-] Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
                            } catch (IOException e) {
                                System.err.println("Lỗi đóng socket: " + e.getMessage());
                            }
                        }
                    });

                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Lỗi khi accept client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to start Server on port: " + port + ": " + e.getMessage());
        }
    }

    private void printPoolStatus() {
        System.out.println("=== THREAD POOL STATUS ===");
        System.out.println("Pool Size: " + threadPool.getPoolSize());
        System.out.println("Active Threads: " + threadPool.getActiveCount());
        System.out.println("Core Pool Size: " + threadPool.getCorePoolSize());
        System.out.println("Maximum Pool Size: " + threadPool.getMaximumPoolSize());
        System.out.println("Queue Size: " + threadPool.getQueue().size());
        System.out.println("Completed Tasks: " + threadPool.getCompletedTaskCount());
        System.out.println("Total Tasks: " + threadPool.getTaskCount());
        System.out.println("==========================\n");
    }

    // Dừng server an toàn
    public void stop() {
        isRunning = false;
        try {
            // Đóng ServerSocket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println("\nĐang dừng server...");

            // Ngừng nhận task mới
            threadPool.shutdown();

            // Chờ task hiện tại hoàn thành
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Buộc dừng các task đang chạy...");
                threadPool.shutdownNow();

                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("ThreadPool không thể dừng hoàn toàn");
                }
            }

            System.out.println("Server đã dừng thành công!");
            System.out.println("Total tasks processed: " + threadPool.getCompletedTaskCount());

        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Lỗi khi dừng server: " + e.getMessage());
        }
    }

    // Custom ThreadFactory để đặt tên thread dễ debug
    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "client-handler-";

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);  // Non-daemon thread
            t.setPriority(Thread.NORM_PRIORITY);
            System.out.println("Tạo thread mới: " + t.getName());
            return t;
        }
    }

    // Custom RejectedExecutionHandler để xử lý khi hàng đợi đầy
    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("⚠️ WARNING: Task bị từ chối! Queue đã đầy. " +
                    "Active threads: " + executor.getActiveCount() +
                    ", Pool size: " + executor.getPoolSize());

            // Có thể thử lại hoặc ghi log, ở đây in ra warning
        }
    }

    // Điều chỉnh dynamic pool size (tính năng đặc biệt của ThreadPoolExecutor)
    public void adjustPoolSize(int newCoreSize, int newMaxSize) {
        if (newCoreSize > 0 && newCoreSize <= newMaxSize) {
            threadPool.setCorePoolSize(newCoreSize);
            threadPool.setMaximumPoolSize(newMaxSize);
            System.out.println("Đã điều chỉnh pool size: Core=" + newCoreSize + ", Max=" + newMaxSize);
        }
    }

    public static void main(String[] args) {
        // Khởi tạo server với ThreadPoolExecutor
        // core=10, max=100, queue=200
        Server server = new Server(12345, 10, 100, 200);

        // Thêm monitor thread để theo dõi
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Mỗi 30 giây
                    server.printPoolStatus();

                    // Ví dụ: tự động điều chỉnh pool size theo tải
                    if (server.threadPool.getQueue().size() > 150) {
                        System.out.println("⚠️ Hàng đợi đang đầy, tăng pool size...");
                        server.adjustPoolSize(20, 150);
                    } else if (server.threadPool.getQueue().size() < 50 &&
                            server.threadPool.getPoolSize() > 10) {
                        System.out.println("Giảm pool size để tiết kiệm tài nguyên...");
                        server.adjustPoolSize(10, 100);
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nNhận tín hiệu tắt server...");
            server.stop();
        }));

        // Khởi động server
        server.start();
    }
}

//package geoinfo.server;
//
//import geoinfo.server.handler.ClientHandler;
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//public class Server {
//    private int port;
//
//    public Server(int port) {
//        this.port = port;
//    }
//
//    public void start() {
//        try(ServerSocket server = new ServerSocket(port)){
//            System.out.println("Server đang lắng nghe tại port " + port);
//            while (true) {
//                Socket socket = server.accept();
//                ClientHandler.handleClient(socket);
//            }
//        } catch (IOException e) {
//            System.out.println("Lỗi khởi tạo " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        Server server = new Server(12345);
//        server.start();
//    }
//}
