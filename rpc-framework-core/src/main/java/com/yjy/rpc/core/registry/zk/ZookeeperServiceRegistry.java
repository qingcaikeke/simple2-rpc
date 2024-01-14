package com.yjy.rpc.core.registry.zk;

import com.yjy.rpc.core.common.ServiceInfo;
import com.yjy.rpc.core.exception.RpcException;
import com.yjy.rpc.core.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
/**
 * Zookeeper 实现服务注册中心类
 */
@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry {
    // Zookeeper连接相关的常量
    //1.会话超时时间，心跳间隔，客户端超过这个时间没有发送心跳，Zookeeper认为1客户端故障
    private static final int SESSION_TIMEOUT = 60 * 1000;
    //2.连接超时时间，在此时间内发起连接到Zookeeper服务器
    private static final int CONNECT_TIMEOUT = 15 * 1000;
    //3.重试的基本等待时间
    private static final int BASE_SLEEP_TIME = 3 * 1000;
    //4.最大重试次数
    private static final int MAX_RETRY = 10;
    //5.服务注册的基础路径，所有服务将在此路径下注册 todo
    private static final String BASE_PATH = "/wxy_rpc";
    //6.Curator框架的核心类，用于建立和管理与Zookeeper的连接，并完成操作，如创建节点、监视节点变化
    private CuratorFramework client;
    //用于服务发现的类,监视服务的注册和注销,获取有关注册服务的信息,泛型标识服务为ServiceInfo
    private ServiceDiscovery<ServiceInfo> serviceDiscovery;


    /**
     * 构造zk客户端，传入 zk 的连接地址，如：127.0.0.1:2181
     *
     * @param registryAddress zookeeper 的连接地址
     */
    public ZookeeperServiceRegistry(String registryAddress) {
        try {
            // 创建Zookeeper客户端示例
            client = CuratorFrameworkFactory
                    .newClient(registryAddress, SESSION_TIMEOUT, CONNECT_TIMEOUT,
                            new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRY));
            // 开启客户端通信
            client.start();

            // 构建 ServiceDiscovery 服务注册中心
            serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .client(client)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))//与zk传输消息需要序列化器
                    .basePath(BASE_PATH)
                    .build();

            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("An error occurred while starting the zookeeper registry: ", e);
        }
    }

    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                    .name(serviceInfo.getServiceName())
                    .address(serviceInfo.getAddress())
                    .port(serviceInfo.getPort())
                    .payload(serviceInfo)
                    .build();
            serviceDiscovery.registerService(serviceInstance);
            log.info("Successfully registered [{}] service.", serviceInstance.getName());
        } catch (Exception e) {
            throw new RpcException(String.format("An error occurred when rpc server registering [%s] service.",
                    serviceInfo.getServiceName()), e);
        }
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) throws Exception {
        ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                .name(serviceInfo.getServiceName())
                .address(serviceInfo.getAddress())
                .port(serviceInfo.getPort())
                .payload(serviceInfo)
                .build();
        serviceDiscovery.unregisterService(serviceInstance);
        log.warn("Successfully unregistered {} service.", serviceInstance.getName());
    }

    @Override
    public void destroy() throws Exception {
        serviceDiscovery.close();
        client.close();
        log.info("Destroy zookeeper registry completed.");
    }
}

