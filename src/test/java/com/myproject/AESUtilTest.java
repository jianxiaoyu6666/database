package com.myproject;

import com.myproject.util.AESUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AESUtilTest {

    @Test
    void testEncryptDecrypt_normal() {
        String original = "Hello123@#密码测试";
        String encrypted = AESUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted); // 密文应该不同于原文

        String decrypted = AESUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
        System.out.println("原文: " + original);
        System.out.println("密文: " + encrypted);
        System.out.println("解密: " + decrypted);
    }

    @Test
    void testEncryptDecrypt_password() {
        // 模拟实际场景：密码加密后解密
        String pwd = "123456";
        String encrypted = AESUtil.encrypt(pwd);
        String decrypted = AESUtil.decrypt(encrypted);
        assertEquals(pwd, decrypted);
    }

    @Test
    void testEncryptDecrypt_idNumber() {
        // 模拟实际场景：身份证号
        String idNo = "320102199001011234";
        String encrypted = AESUtil.encrypt(idNo);
        String decrypted = AESUtil.decrypt(encrypted);
        assertEquals(idNo, decrypted);
    }

    @Test
    void testEncryptDecrypt_phone() {
        // 模拟实际场景：手机号
        String phone = "13812345678";
        String encrypted = AESUtil.encrypt(phone);
        String decrypted = AESUtil.decrypt(encrypted);
        assertEquals(phone, decrypted);
    }

    @Test
    void testEncrypt_null() {
        // null 原样返回
        assertNull(AESUtil.encrypt(null));
        assertNull(AESUtil.decrypt(null));
    }

    @Test
    void testEncrypt_empty() {
        // 空字符串原样返回
        assertEquals("", AESUtil.encrypt(""));
        assertEquals("", AESUtil.decrypt(""));
    }

    @Test
    void testEncrypt_sameInputDifferentOutput() {
        // 同一条数据加密两次，密文应不同（因为随机IV），但都能正确解密
        String original = "test123";
        String enc1 = AESUtil.encrypt(original);
        String enc2 = AESUtil.encrypt(original);

        assertNotEquals(enc1, enc2); // IV 不同，密文不同
        assertEquals(original, AESUtil.decrypt(enc1));
        assertEquals(original, AESUtil.decrypt(enc2));
    }

    @Test
    void testDecrypt_invalidData() {
        // 非法密文应抛异常
        assertThrows(RuntimeException.class, () -> {
            AESUtil.decrypt("这不是合法密文");
        });
    }
}
