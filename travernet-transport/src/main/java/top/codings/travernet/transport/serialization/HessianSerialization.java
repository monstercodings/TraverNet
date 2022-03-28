package top.codings.travernet.transport.serialization;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class HessianSerialization implements Serialization {
    public <T> byte[] serialize(T obj) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(os);
            hessianOutput.writeObject(obj);
            return os.toByteArray();
        }
    }

    public <T> T deSerialize(byte[] data, Class<T> clazz) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            HessianInput hessianInput = new HessianInput(is);
            return (T) hessianInput.readObject(clazz);
        }
    }
}
