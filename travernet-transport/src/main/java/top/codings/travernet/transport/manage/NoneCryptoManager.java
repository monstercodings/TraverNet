package top.codings.travernet.transport.manage;

public class NoneCryptoManager implements CryptoManager {

    @Override
    public byte[] encrypt(byte[] data) {
        return data;
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return data;
    }

    @Override
    public void resetPassword(String password) {
    }
}
