package geoinfo.server.network;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public final class ServerRegistryApi {
    private static final String RETOOL_API = System.getProperty(
            "geoinfo.registry.url",
            "https://retoolapi.dev/Wakdeo/data/1"
    );
    private static final String DEFAULT_IP = System.getProperty("geoinfo.default.host", "localhost");
    private static final int DEFAULT_PORT = Integer.getInteger("geoinfo.default.port", 12345);

    private ServerRegistryApi() {
    }

    public static ServerEndpoint fetchServerEndpoint() throws IOException {
        String body = Jsoup.connect(RETOOL_API)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .header("Content-Type", "application/json")
                .method(Connection.Method.GET)
                .execute()
                .body();

        JSONObject json = new JSONObject(body);
        String ip = firstNonBlank(json.optString("ip"), json.optString("host"), DEFAULT_IP);
        int port = parsePort(json.opt("port"), DEFAULT_PORT);
        return new ServerEndpoint(ip, port);
    }

    public static ServerEndpoint fetchServerEndpointOrDefault() {
        try {
            return fetchServerEndpoint();
        } catch (Exception ex) {
            return defaultEndpoint();
        }
    }

    public static void publishServerEndpoint(ServerEndpoint endpoint) throws IOException {
        JSONObject payload = new JSONObject()
                .put("ip", endpoint.ip())
                .put("host", endpoint.ip())
                .put("port", endpoint.port());

        Jsoup.connect(RETOOL_API)
                .ignoreContentType(true)
                .header("Content-Type", "application/json")
                .requestBody(payload.toString())
                .method(Connection.Method.PUT)
                .execute();
    }

    public static ServerEndpoint defaultEndpoint() {
        return new ServerEndpoint(DEFAULT_IP, DEFAULT_PORT);
    }

    public static String resolveServerHost() {
        String override = System.getProperty("geoinfo.server.host");
        if (override != null && !override.isBlank()) {
            return override.trim();
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 53);
            String address = socket.getLocalAddress().getHostAddress();
            if (isUsableHost(address)) {
                return address;
            }
        } catch (Exception ignored) {
        }

        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && isUsableHost(address.getHostAddress())) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return DEFAULT_IP;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return DEFAULT_IP;
    }

    private static int parsePort(Object rawPort, int fallbackPort) {
        if (rawPort == null) {
            return fallbackPort;
        }

        if (rawPort instanceof Number number) {
            int parsed = number.intValue();
            return parsed >= 1 && parsed <= 65535 ? parsed : fallbackPort;
        }

        try {
            int parsed = Integer.parseInt(rawPort.toString().trim());
            return parsed >= 1 && parsed <= 65535 ? parsed : fallbackPort;
        } catch (NumberFormatException ex) {
            return fallbackPort;
        }
    }

    private static boolean isUsableHost(String ip) {
        return ip != null
                && !ip.isBlank()
                && !"0.0.0.0".equals(ip)
                && !"127.0.0.1".equals(ip)
                && !"localhost".equalsIgnoreCase(ip);
    }
}
