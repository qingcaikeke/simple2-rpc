package com.yjy.rpc.core.discovery.zk;


import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.common.ServiceInfo;
import com.yjy.rpc.core.discovery.ServiceDiscovery;
import com.yjy.rpc.core.exception.RpcException;
import com.yjy.rpc.core.loadbalance.LoadBalance;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Zookeeper 实现服务发现实现类
 * 逻辑为，接收请求，根据服务名，找到提供者列表，根据负载均衡选一个
 * 提供者列表存在zook中，但是本地有一个缓存，只要不是第一次查找某个服务，一般直接去缓存拿
 * 但是服务注册是注册到zk，所以用一个serviceDiscovery创建了一个serviceCache服务
 * 给serviceCache添加一个监听器监听服务是否发生变化，发生变化需要更新缓存
 * 同时如果zk挂了，通过本地缓存依旧可以查找服务
 * @see org.apache.curator.framework.CuratorFramework
 * @see org.apache.curator.x.discovery.ServiceDiscovery
 * @see org.apache.curator.x.discovery.ServiceCache
 */
@Slf4j
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
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
    //6.负载均衡
    private LoadBalance loadBalance;
    //7.Curator框架的核心类，用于建立和管理与Zookeeper的连接，并完成操作，如创建节点、监视节点变化
    private CuratorFramework client;
    //8.用于服务发现的类,监视服务的注册和注销,获取有关注册服务的信息,泛型标识服务为ServiceInfo
    private org.apache.curator.x.discovery.ServiceDiscovery<ServiceInfo> serviceDiscovery;


    /**
     * ServiceCache:  Curator 提供的服务缓存工具，用于监听服务实例的变化
     * 通过添加 ServiceCacheListener 监听器，当服务实例发生变化时，会触发 cacheChanged 方法，该方法中更新了本地缓存的服务列表。
     * 将在zk中的服务数据缓存至本地，并监听服务变化，实时更新缓存
     * 服务本地缓存，将服务缓存到本地并增加 watch 事件，当远程服务发生改变时自动更新服务缓存
     */
    private final Map<String, ServiceCache<ServiceInfo>> serviceCacheMap = new ConcurrentHashMap<>();

    /**
     * 用来将服务列表缓存到本地内存，当服务发生变化时，由 serviceCache 进行服务列表更新操作，当 zk 挂掉时，将保存当前服务列表以便继续提供服务
     */
    private final Map<String, List<ServiceInfo>> serviceMap = new ConcurrentHashMap<>();


    /**
     * 构造方法，传入 zk 的连接地址，如：127.0.0.1:2181
     *
     * @param registryAddress zookeeper 的连接地址
     */
    public ZookeeperServiceDiscovery(String registryAddress, LoadBalance loadBalance) {
        try {
            this.loadBalance = loadBalance;

            // 创建zk客户端示例
            client = CuratorFrameworkFactory
                    .newClient(registryAddress, SESSION_TIMEOUT, CONNECT_TIMEOUT,
                            new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRY));
            // 开启客户端通信
            client.start();

            // 构建 ServiceDiscovery 服务注册中心
            serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .client(client)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))
                    .basePath(BASE_PATH)
                    .build();
            // 开启 服务发现
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("An error occurred while starting the zookeeper discovery: ", e);
        }
    }

    /**
     *  根据服务名找到服务地址，使用负载均衡选择，返回服务信息
     */

    @Override
    public ServiceInfo discover(RpcRequest request) {
        try {
            return loadBalance.select(getServices(request.getServiceName()), request);
        } catch (Exception e) {
            throw new RpcException(String.format("Remote service discovery did not find service %s.",
                    request.getServiceName()), e);
        }
    }

    /**
     * ServiceCache:  Curator 提供的服务缓存工具，用于监听服务实例的变化
     * 通过添加 ServiceCacheListener 监听器，当服务实例发生变化时，会触发 cacheChanged 方法，该方法中更新了本地缓存的服务列表
     */
    @Override
    public List<ServiceInfo> getServices(String serviceName) throws Exception {
        if (!serviceMap.containsKey(serviceName)) {
            // 构建本地服务缓存
            ServiceCache<ServiceInfo> serviceCache = serviceDiscovery.serviceCacheBuilder()
                    .name(serviceName)
                    .build();
            // 添加服务监听，当服务发生变化时主动更新本地缓存并通知
            serviceCache.addListener(new ServiceCacheListener() {
                @Override
                public void cacheChanged() {
                    log.info("The service [{}] cache has changed. The current number of service samples is {}."
                            , serviceName, serviceCache.getInstances().size());
                    // 更新本地缓存的服务列表
                    serviceMap.put(serviceName, serviceCache.getInstances().stream()
                            .map(ServiceInstance::getPayload)
                            .collect(Collectors.toList()));
                }

                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    // 当连接状态发生改变时，只打印提示信息，保留本地缓存的服务列表
                    log.info("The client {} connection status has changed. The current status is: {}."
                            , client, newState);
                }
            });
            // 开启服务缓存监听
            serviceCache.start();
            // 将服务缓存对象存入本地
            serviceCacheMap.put(serviceName, serviceCache);
            // 将服务列表缓存到本地
            serviceMap.put(serviceName, serviceCacheMap.get(serviceName).getInstances()
                    .stream()
                    .map(ServiceInstance::getPayload)
                    .collect(Collectors.toList()));
        }
        return serviceMap.get(serviceName);
    }

    @Override
    public void destroy() throws Exception {
        for (ServiceCache<ServiceInfo> serviceCache : serviceCacheMap.values()) {
            if (serviceCache != null) {
                //todo ：serviceCache是什么？为什么也要关闭
                //Curator 提供的服务缓存工具，用于监听服务实例的变化
                serviceCache.close();
            }
        }
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }
        if (client != null) {
            client.close();
        }
    }
}

