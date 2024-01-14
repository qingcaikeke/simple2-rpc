package com.yjy.rpc.server.serviceCache;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 本地服务缓存类，将提供的服务实体类缓存到本地(建立一个接口，实现类的map)
 */
@Slf4j
public class LocalServiceCache {
    /**
     * zkServiceRegistry是把服务信息（接口名和ip地址）ServiceInfo注册到zk
     * 这个serviceMap与之无关，是接收到请求后，去找对应的实现类（因为可以有多个实现类，只使用加了注解的那个）
     */
    private static final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    public static void addService(String serviceName, Object obj) {
        serviceMap.put(serviceName, obj);
        log.info("Service [{}] was successfully added to the local cache.", serviceName);
    }

    public static Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }

    public static void removeService(String serviceName) {
        serviceMap.remove(serviceName);
        log.info("Service [{}] was removed from local cache", serviceName);
    }

}

