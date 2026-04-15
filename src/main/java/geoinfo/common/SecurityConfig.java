package geoinfo.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecurityConfig {
    private static Properties prop = new Properties();

    static {
        try (InputStream input = SecurityConfig.class.getClassLoader().getResourceAsStream("security.properties")) {
            if (input != null) {
                prop.load(input);
            }
        } catch (IOException e) {
            System.err.println("Error loading properties file");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPrivateKey() {
        return prop.getProperty("server.private_key");
    }

    public static String getPublicKey() {
        return prop.getProperty("client.public_key");
    }
}
