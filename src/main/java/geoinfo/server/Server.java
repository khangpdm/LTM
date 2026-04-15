package geoinfo.server;

import geoinfo.server.handler.ClientHandler;
import geoinfo.server.network.ServerEndpoint;
import geoinfo.server.network.ServerRegistryApi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private int port;
    private final ThreadPoolExecutor threadPool;
    private volatile boolean isRunning;
    private ServerSocket serverSocket;

    public Server(int port, int corePoolSize, int maxPoolSize, int queueCapacity) {
        this.port = port;
        this.isRunning = true;
        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),  // Hàng đợi chờ
                new CustomThreadFactory(),
                new CustomRejectedExecutionHandler()
        );
        threadPool.allowCoreThreadTimeOut(true);
    }

    public Server(int port) {
        this(port, 10, 50, 100);  // core=10, max=50, queue=100
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            port = serverSocket.getLocalPort();
            publishServerEndpoint();

            System.out.println("||=========================================================||");
            System.out.printf("|| %-55s ||%n", "The Server is listening at the port: " + port);
            System.out.println("||=========================================================||");
            System.out.printf("|| %-55s ||%n", "  - Core Pool Size: " + threadPool.getCorePoolSize());
            System.out.printf("|| %-55s ||%n", "  - Maximum Pool Size: " + threadPool.getMaximumPoolSize());
            System.out.printf("|| %-55s ||%n", "  - Queue Capacity: " + threadPool.getQueue().remainingCapacity());
            System.out.printf("|| %-55s ||%n", "  - Keep Alive Time: 60 seconds");
            System.out.println("||=========================================================||");
            System.out.println();

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[+] Client connected: "
                            + clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());

                    printPoolStatus();
                    threadPool.execute(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to start server on port " + port + ": " + e.getMessage());
        }
    }

    private void publishServerEndpoint() {
        try {
            ServerEndpoint endpoint = new ServerEndpoint(ServerRegistryApi.resolveServerHost(), port);
            ServerRegistryApi.publishServerEndpoint(endpoint);
            System.out.println("Published server endpoint to registry: " + endpoint.asAddress());
        } catch (IOException e) {
            System.err.println("Unable to publish server endpoint: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            ClientHandler.handleClient(clientSocket);
        } catch (Exception e) {
            System.err.println("Client handling error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("[-] Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("Socket close error: " + e.getMessage());
            }
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
        System.out.println("==========================");
        System.out.println();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println();
            System.out.println("Stopping server...");
            threadPool.shutdown();

            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Forcing running tasks to stop...");
                threadPool.shutdownNow();

                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("ThreadPool did not stop cleanly");
                }
            }

            System.out.println("Server stopped successfully");
            System.out.println("Total tasks processed: " + threadPool.getCompletedTaskCount());
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Stop server error: " + e.getMessage());
        }
    }

    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "client-handler-";

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            System.out.println("Created thread: " + thread.getName());
            return thread;
        }
    }

    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            System.err.println("WARNING: Task rejected. Queue is full. Active threads: "
                    + executor.getActiveCount() + ", Pool size: " + executor.getPoolSize());
        }
    }

    public void adjustPoolSize(int newCoreSize, int newMaxSize) {
        if (newCoreSize > 0 && newCoreSize <= newMaxSize) {
            threadPool.setCorePoolSize(newCoreSize);
            threadPool.setMaximumPoolSize(newMaxSize);
            System.out.println("Adjusted pool size: Core=" + newCoreSize + ", Max=" + newMaxSize);
        }
    }

    public static void main(String[] args) {
        int dynamicPort = Integer.getInteger("geoinfo.server.port", 0);
        Server server = new Server(dynamicPort, 10, 100, 200);

        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    server.printPoolStatus();

                    if (server.threadPool.getQueue().size() > 150) {
                        System.out.println("Queue is filling up, increasing pool size...");
                        server.adjustPoolSize(20, 150);
                    } else if (server.threadPool.getQueue().size() < 50
                            && server.threadPool.getPoolSize() > 10) {
                        System.out.println("Reducing pool size to save resources...");
                        server.adjustPoolSize(10, 100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("Shutdown signal received");
            server.stop();
        }));

        server.start();
    }

    public static void retoolServer(int port) throws IOException {
        ServerEndpoint endpoint = new ServerEndpoint(ServerRegistryApi.resolveServerHost(), port);
        ServerRegistryApi.publishServerEndpoint(endpoint);
    }
}
