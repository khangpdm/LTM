package geoinfo.common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class SecurityUtils {
    // ham ma hoa key aes bang thuat toan rsa su dung public key cua server de ma hoa ( ham de client su dung )
    public static String encryptRSA(byte[] data, String publicKeyStr) throws Exception {
        // chuyen chuoi public key tu dang byte base64 sang dang byte tho de keyfactory xu ly
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
        // dinh dang lai byte[] theo chuan x509 cho public key
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        // tao public key
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

        // ma hoa du lieu
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        // chuyen lai thanh dang base64 de gui du lieu
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    // ham de server giai ma khoa aes
    public static byte[] decryptRSA(String encryptedData, String privateKeyStr) throws Exception {
        // chuyen tu dang base 64 sang tho
        byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
        // dinh dang theo chuan PKCS8 danh cho private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        //giai ma rsa de lay key aes
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.getDecoder().decode(encryptedData));
    }

    // aes -- client va server su dung chung 1 khoa
    public static String encryptAES(String data, byte[] key) throws Exception {
        // tao key tu byte[]
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        // ecb : che dooo ma hoa tung khoi
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ghi ro mode de client va server dong bo
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    public static String decryptAES(String encryptedData, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
    }

    public static byte[] generateAESKey() throws Exception {
        javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
        kg.init(128); // thiet lap do dai cua aes key
        return kg.generateKey().getEncoded(); // tao 1 key ngau nhien va chuyen thanh dang tho
    }
}