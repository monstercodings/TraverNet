package top.codings.travernet.transport.compress;

public class EmptyCompress implements Compress {

    @Override
    public byte[] compress(byte[] data) {
        return data;
    }

    @Override
    public byte[] uncompress(byte[] data) {
        return data;
    }
}
