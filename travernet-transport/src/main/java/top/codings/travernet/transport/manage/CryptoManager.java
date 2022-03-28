package top.codings.travernet.transport.manage;

public interface CryptoManager {
    byte[] encrypt(byte[] data);

    byte[] decrypt(byte[] data);

    void resetPassword(String password);
}
