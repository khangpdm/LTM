package geoinfo.server.network;

public record ServerEndpoint(String ip, int port) {
    public ServerEndpoint {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("Host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        ip = ip.trim();
    }

    public String asAddress() {
        return ip + ":" + port;
    }
}
