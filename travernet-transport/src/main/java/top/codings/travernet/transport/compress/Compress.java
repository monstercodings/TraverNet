package top.codings.travernet.transport.compress;

public interface Compress {
    byte[] compress(byte[] data);

    byte[] uncompress(byte[] data);
}
