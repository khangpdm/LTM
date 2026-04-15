package geoinfo.common;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class Setup {
    public static void generateAndPrintRSAKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        System.out.println("Public Key (Dán vào Client): " + Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        System.out.println("Private Key (Lưu tại Server): " + Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
    }
    public static void main(String[] args) throws Exception {
        generateAndPrintRSAKeys();
    }
}
