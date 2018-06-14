package com.andyyan.cache.sync.rocketmq.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by zhaoyalong on 16-12-6.
 */
public class KryoSerializer {

    private static final KryoPool pool = new KryoPool.Builder(new KryoFactory() {
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.addDefaultSerializer(String.class, DefaultSerializers.StringSerializer.class);
            kryo.addDefaultSerializer(Map.class, DefaultSerializers.CollectionsSingletonMapSerializer.class);
            // configure kryo instance, customize settings
            return kryo;
        }
    }).softReferences().build();

    public static byte[] encode(String o) {
        return pool.run(new MessageEncodeString2ByteArrCallback(o));
    }

    public static byte[] encode(Map o) {
        return pool.run(new MessageEncodeMap2ByteArrCallback(o));
    }

    public static Map decodeByte2Map(byte[] o) {
        return pool.run(new MessageDecodeByteArr2MapCallback(o));
    }

    public static String decodeByte2String(byte[] o) {
        return pool.run(new MessageDecodeByteArr2StringCallback(o));
    }

    static class MessageDecodeByteArr2StringCallback implements KryoCallback<String> {

        private byte[] msgBody;

        MessageDecodeByteArr2StringCallback(byte[] msgBody) {
            this.msgBody = msgBody;
        }

        public String execute(Kryo kryo) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(msgBody);
            Input input = new Input(inputStream, 4096);
            return kryo.readObject(input, String.class);
        }
    }

    static class MessageDecodeByteArr2MapCallback implements KryoCallback<Map> {

        private byte[] msgBody;

        MessageDecodeByteArr2MapCallback(byte[] msgBody) {
            this.msgBody = msgBody;
        }

        public Map execute(Kryo kryo) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(msgBody);
            Input input = new Input(inputStream, 4096);
            return kryo.readObject(input, Map.class);
        }
    }


    static class MessageEncodeString2ByteArrCallback implements KryoCallback<byte[]> {

        private String message;

        MessageEncodeString2ByteArrCallback(String message) {
            this.message = message;
        }

        public byte[] execute(Kryo kryo) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Output output = new Output(outStream, 4096);
            kryo.writeObject(output, message);
            return output.getBuffer();
        }
    }

    static class MessageEncodeMap2ByteArrCallback implements KryoCallback<byte[]> {

        private Map message;

        MessageEncodeMap2ByteArrCallback(Map message) {
            this.message = message;
        }

        public byte[] execute(Kryo kryo) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Output output = new Output(outStream, 4096);
            kryo.writeObject(output, message);
            return output.getBuffer();
        }
    }

    public static void main(String[] args) {
        String a = "12321321321321323213213213213";
        Map m = new HashMap();
        m.put("a",a);
        System.out.println(encode(a).length);
        System.out.println(encode(m).length);
    }
}
