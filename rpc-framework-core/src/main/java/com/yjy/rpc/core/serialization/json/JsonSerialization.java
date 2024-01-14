package com.yjy.rpc.core.serialization.json;

import com.yjy.rpc.core.serialization.Serialization;

public class JsonSerialization implements Serialization {
    @Override
    public <T> byte[] serialize(T object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return null;
    }
}
