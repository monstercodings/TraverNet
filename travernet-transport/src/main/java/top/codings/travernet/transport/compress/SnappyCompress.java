package top.codings.travernet.transport.compress;

import cn.hutool.extra.compress.CompressException;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

import java.io.IOException;

@Slf4j
public class SnappyCompress implements Compress {
    @Override
    public byte[] compress(byte[] data) throws CompressException {
        try {
            byte[] compress = Snappy.compress(data);
            if (log.isTraceEnabled()) {
                float v = (data.length - compress.length) * 1f / data.length;
                log.trace("传输数据压缩率 -> {}", String.format("%.2f", v));
            }
            return compress;
        } catch (IOException e) {
            throw new CompressException("压缩数据失败", e);
        }
    }

    @Override
    public byte[] uncompress(byte[] data) throws CompressException {
        try {
            return Snappy.uncompress(data);
        } catch (IOException e) {
            throw new CompressException("解压数据失败", e);
        }
    }
}
