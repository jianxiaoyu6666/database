package com.myproject.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Key;
import java.util.Base64;

//这部分借AI写的，工具类，了解他是怎么加密解密的然后直接调用静态方法
public class AESUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private static final String SECRET_KEY = "key";

    // 固定密钥: 对 SECRET_KEY 做 SHA-256 取前 32 字节，保证加解密用同一把钥匙
    private static final Key FIXED_KEY;
    static {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            FIXED_KEY = new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("初始化AES固定密钥失败", e);
        }
    }

    //加密（把前端传入的密码/身份证号/手机号加密后再存入数据库）
    //用法应该有两个地方——1.注册时 2.用户修改这3个需要加密的信息时
    public static String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, FIXED_KEY, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, result, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败: " + e.getMessage(), e);
        }
    }

    //解密（把已经加密的用户手机号、密码、身份证号解密出来）
    public static String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decodedBytes, 0, iv, 0, iv.length);

            byte[] encryptedBytes = new byte[decodedBytes.length - iv.length];
            System.arraycopy(decodedBytes, iv.length, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, FIXED_KEY, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败: " + e.getMessage(), e);
        }
    }
}

