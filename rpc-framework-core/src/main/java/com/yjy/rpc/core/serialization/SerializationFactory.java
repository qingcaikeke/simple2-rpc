package com.yjy.rpc.core.serialization;

import com.yjy.rpc.core.protocol.enums.SerializationType;
import com.yjy.rpc.core.serialization.hessian.HessianSerialization;
import com.yjy.rpc.core.serialization.json.JsonSerialization;
import com.yjy.rpc.core.serialization.kryo.KryoSerialization;

public class SerializationFactory {
    public static Serialization getSerialization(SerializationType enumType) {
        switch (enumType) {
            case JSON:
                return new JsonSerialization();
            case HESSIAN:
                return new HessianSerialization();
            case KRYO:
                return new KryoSerialization();
            default:
                throw new IllegalArgumentException(String.format("The serialization type %s is illegal.",
                        enumType.name()));
        }
    }
}
