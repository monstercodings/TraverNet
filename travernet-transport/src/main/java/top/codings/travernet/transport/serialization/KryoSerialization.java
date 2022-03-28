package top.codings.travernet.transport.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.*;
import java.util.Map;

public class KryoSerialization implements Serialization {
    private final static ThreadLocal<Inner> INNER_THREAD_LOCAL = ThreadLocal.withInitial(Inner::new);
    private final Map<Class, String> classToKeyMap;

    public KryoSerialization(Map<Class, String> classToKeyMap) {
        this.classToKeyMap = classToKeyMap;
    }

    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        Inner inner = getInner();
        Output output = new Output(new BufferedOutputStream(new ByteArrayOutputStream()));
        inner.kryo.writeObject(output, obj);
        byte[] bytes = output.toBytes();
        output.close();
        return bytes;
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clz) throws IOException {
        Inner inner = getInner();
        Input input = new Input(new BufferedInputStream(new ByteArrayInputStream(data)));
        T t = inner.kryo.readObject(input, clz);
        input.close();
        return t;
    }

    private Inner getInner() {
        Inner inner = INNER_THREAD_LOCAL.get();
        if (!inner.init) {
            inner.init = true;
            if (classToKeyMap != null) {
                classToKeyMap.forEach((aClass, s) -> inner.kryo.register(aClass, Integer.valueOf(s)));
            }
        }
        return inner;
    }

    private static class Inner {
        Kryo kryo = new Kryo();
        boolean init;

        public Inner() {
            kryo.setRegistrationRequired(false);
//            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        }
    }

}
