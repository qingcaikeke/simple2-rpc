package com.yjy.rpc.core.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取单实例对象的工厂,RpcRequestHandler,HttpRpcRequestHandler,ChannelProvider是单例的
 * 双重锁定：私有构造方法，公有获取实例方法，判断是不是null，不是则加锁，再判断是不是null，是的化调用构造方法
 */
public class SingletonFactory {
    public static final Map<String,Object> OBJECT_MAP = new ConcurrentHashMap<>();

    public static <T> T getInstance(Class<T> clazz) {
        try {
            String name = clazz.getName();
            if(OBJECT_MAP.containsKey(name)){
                return clazz.cast(OBJECT_MAP.get(name));
            }else {
                T instance = clazz.getDeclaredConstructor().newInstance();
                OBJECT_MAP.put(name,instance);
                return instance;
            }
        }catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
