package top.codings.travernet.transport.manage;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.error.TraverConfigurationException;
import top.codings.travernet.common.error.TraverCryptoException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Slf4j
public class AesGcmCryptoManager implements CryptoManager {
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/GCM/NoPadding";// 默认的加密算法
    private String encryptPass;
    private final int keySize;

    public AesGcmCryptoManager(String encryptPass, int keySize) {
        if (StrUtil.isBlank(encryptPass)) {
            throw new TraverConfigurationException("加密模式为密钥时password不能为空");
        }
        this.encryptPass = encryptPass;
        this.keySize = keySize;
    }

    /**
     * AES 加密操作
     *
     * @param data 待加密内容
     * @return 返回Base64转码后的加密数据
     */
    @Override
    public byte[] encrypt(byte[] data) throws TraverCryptoException {
        try {
            byte[] iv = new byte[12];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            GCMParameterSpec params = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(encryptPass), params);
            byte[] encryptData = cipher.doFinal(data);
            assert encryptData.length == data.length + 16;
            byte[] message = new byte[12 + data.length + 16];
            System.arraycopy(iv, 0, message, 0, 12);
            System.arraycopy(encryptData, 0, message, 12, encryptData.length);
            return message;
        } catch (Exception e) {
            throw new TraverCryptoException("加密内容失败", e);
        }
    }

    /**
     * AES 解密操作
     *
     * @param data
     * @return
     */
    public byte[] decrypt(byte[] data) throws TraverCryptoException {
        try {
            if (data.length < 12 + 16)
                throw new IllegalArgumentException();
            GCMParameterSpec params = new GCMParameterSpec(128, data, 0, 12);
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(encryptPass), params);
            byte[] decryptData = cipher.doFinal(data, 12, data.length - 12);
            return decryptData;
        } catch (Exception e) {
            throw new TraverCryptoException("解密内容失败", e);
        }
    }

    @Override
    public void resetPassword(String password) {
        this.encryptPass = password;
    }

    /**
     * 生成加密秘钥
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    private SecretKeySpec getSecretKey(String encryptPass) throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
        // 初始化密钥生成器，AES要求密钥长度为128位、192位、256位
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(encryptPass.getBytes());
        kg.init(keySize, secureRandom);
        SecretKey secretKey = kg.generateKey();
        return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
    }
}
