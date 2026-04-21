package geoinfo.server;

import geoinfo.server.handler.ClientHandler;
import geoinfo.server.network.ServerEndpoint;
import geoinfo.server.network.ServerRegistryApi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

            logInfo("Server started on port " + port);
            logInfo("ThreadPool config: core=" + threadPool.getCorePoolSize()
                    + ", max=" + threadPool.getMaximumPoolSize()
                    + ", queueCapacity=" + (threadPool.getQueue().size() + threadPool.getQueue().remainingCapacity())
                    + ", keepAlive=60s");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logInfo("Client connected: "
                            + clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());

                    printPoolStatus();
                    threadPool.execute(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        logError("Accept error", e);
                    }
                }
            }
        } catch (IOException e) {
            logError("Unable to start server on port " + port, e);
        }
    }

    private void publishServerEndpoint() {
        try {
            ServerEndpoint endpoint = new ServerEndpoint(ServerRegistryApi.resolveServerHost(), port);
            ServerRegistryApi.publishServerEndpoint(endpoint);
            logInfo("Published server endpoint: " + endpoint.asAddress());
        } catch (IOException e) {
            logError("Unable to publish server endpoint", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            ClientHandler.handleClient(clientSocket);
        } catch (Exception e) {
            logError("Client handling error", e);
        } finally {
            try {
                clientSocket.close();
                logInfo("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                logError("Socket close error", e);
            }
        }
    }

    private void printPoolStatus() {
        logInfo("ThreadPool status: pool=" + threadPool.getPoolSize()
                + ", active=" + threadPool.getActiveCount()
                + ", core=" + threadPool.getCorePoolSize()
                + ", max=" + threadPool.getMaximumPoolSize()
                + ", queue=" + threadPool.getQueue().size()
                + ", completed=" + threadPool.getCompletedTaskCount()
                + ", total=" + threadPool.getTaskCount());
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            logInfo("Stopping server...");
            threadPool.shutdown();

            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                logWarn("Graceful shutdown timeout, forcing running tasks to stop");
                threadPool.shutdownNow();

                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    logError("ThreadPool did not stop cleanly");
                }
            }

            logInfo("Server stopped successfully");
            logInfo("Total tasks processed: " + threadPool.getCompletedTaskCount());
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logError("Stop server error", e);
        }
    }

    private static String logPrefix(String level) {
        return "[" + LocalDateTime.now().format(LOG_TIME_FORMAT) + "] [" + level + "] [SERVER] ";
    }

    private static void logInfo(String message) {
        System.out.println(logPrefix("INFO") + message);
    }

    private static void logWarn(String message) {
        System.out.println(logPrefix("WARN") + message);
    }

    private static void logError(String message) {
        System.err.println(logPrefix("ERROR") + message);
    }

    private static void logError(String message, Exception exception) {
        logError(message + ": " + exception.getMessage());
    }

    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "client-handler-";

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            logInfo("Created thread: " + thread.getName());
            return thread;
        }
    }

    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            logWarn("Task rejected, queue is full. Active threads: "
                    + executor.getActiveCount() + ", Pool size: " + executor.getPoolSize());
        }
    }

    public void adjustPoolSize(int newCoreSize, int newMaxSize) {
        if (newCoreSize > 0 && newCoreSize <= newMaxSize) {
            threadPool.setCorePoolSize(newCoreSize);
            threadPool.setMaximumPoolSize(newMaxSize);
            logInfo("Adjusted pool size: core=" + newCoreSize + ", max=" + newMaxSize);
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
                        logWarn("Queue is filling up, increasing pool size");
                        server.adjustPoolSize(20, 150);
                    } else if (server.threadPool.getQueue().size() < 50
                            && server.threadPool.getPoolSize() > 10) {
                        logInfo("Queue is stable, reducing pool size");
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
            logInfo("Shutdown signal received");
            server.stop();
        }));

        server.start();
    }

    public static void retoolServer(int port) throws IOException {
        ServerEndpoint endpoint = new ServerEndpoint(ServerRegistryApi.resolveServerHost(), port);
        ServerRegistryApi.publishServerEndpoint(endpoint);
    }
}
