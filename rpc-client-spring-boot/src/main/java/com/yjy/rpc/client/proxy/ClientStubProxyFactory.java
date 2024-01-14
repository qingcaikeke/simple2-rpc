package com.yjy.rpc.client.proxy;

import com.yjy.rpc.client.config.RpcClientProperties;
import com.yjy.rpc.client.transport.RpcClient;
import com.yjy.rpc.core.discovery.ServiceDiscovery;
import com.yjy.rpc.core.util.ServiceUtil;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientStubProxyFactory {
    /**
     * 服务发现中心实现类：连接zk客户端，通过服务名得到list<ServiceInfo>，使用负载均衡得到一个ServiceInfo(服务名加地址)
     * 区分到客户端那边后另一个发现（已经到了ServiceInfo的地址），客户端有一个map将接口名与实现类对应
     */
    private final ServiceDiscovery discovery;

    /**
     * RpcClient 传输实现类
     */
    private final RpcClient rpcClient;

    /**
     * 客户端配置属性
     */
    private final RpcClientProperties properties;

    public ClientStubProxyFactory(ServiceDiscovery discovery, RpcClient rpcClient, RpcClientProperties properties) {
        this.discovery = discovery;
        this.rpcClient = rpcClient;
        this.properties = properties;
    }

    /**
     * 代理对象缓存
     */
    private static final Map<String, Object> proxyMap = new ConcurrentHashMap<>();


    /**
     * 根据接口信息，获取代理对象
     */
    public <T> T getProxy(Class<T> clazz, String version) {
        // map.computeIfAbsent(key, k -> new Value(f(k)));
        //key不存在，就根据映射函数生成一个value，并存到map，存在则直接返回
        Object proxy = proxyMap.computeIfAbsent(ServiceUtil.serviceKey(clazz.getName(), version), serviceName -> {
            // 如果目标类是一个接口或者 是 java.lang.reflect.Proxy 的子类 则默认使用 JDK 动态代理
            if (clazz.isInterface() || Proxy.isProxyClass(clazz)) {
                return Proxy.newProxyInstance(clazz.getClassLoader(),
                        new Class[]{clazz}, // 注意，这里的接口是 clazz 本身（即，要代理的实现类所实现的接口）
                        new ClientStubInvocationHandler(discovery, rpcClient, properties, serviceName));
            } else { // 使用 CGLIB 动态代理
                // 创建动态代理增加类
                Enhancer enhancer = new Enhancer();
                // 设置类加载器
                enhancer.setClassLoader(clazz.getClassLoader());
                // 设置被代理类
                enhancer.setSuperclass(clazz);
                // 设置方法拦截器
                enhancer.setCallback(new ClientStubMethodInterceptor(discovery, rpcClient, properties, serviceName));
                // 创建代理类
                return enhancer.create();
            }
        });
        return (T) proxy;
    }

}


}
