package com.yjy.rpc.client.config;

import lombok.Data;

@Data
//@ConfigurationProperties(prefix = "rpc.client")
public class RpcClientProperties {
    /**
     * 负载均衡算法：random，一致性哈希
     */
    private String loadbalance;
    /**
     * 序列化算法：JDK, JSON, HESSIAN, KRYO, PROTOSTUFF
     */
    private String serialization;
    /**
     * 传输协议：netty,http
     */
    private String transport;
    /**
     * 注册中心：zk，nacos
     */
    private String registry;
    /**
     * 注册中心地址：如 127.0.0.1:2181
     */
    private String registryAddr;
    /**
     * Connection timeout, default: 5000
     * metadata中的那个超时事件
     */
    private Integer timeout;

    /**
     * 默认初始化
     */
    public RpcClientProperties() {
        this.loadbalance = "random";
        this.serialization = "HESSIAN";
        this.transport = "netty";
        this.registry = "zookeeper";
        this.registryAddr = "127.0.0.1:2181";
        this.timeout = 5000;
    }

}
