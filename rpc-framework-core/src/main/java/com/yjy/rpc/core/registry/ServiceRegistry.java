package com.yjy.rpc.core.registry;

import com.yjy.rpc.core.common.ServiceInfo;

public interface ServiceRegistry {

    /**
     * 注册/重新注册一个服务信息到 注册中心
     *
     * @param serviceInfo 服务信息
     */
    void register(ServiceInfo serviceInfo) throws Exception;

    /**
     * 解除注册/移除一个服务信息从 注册中心
     *
     * @param serviceInfo 服务信息
     */
    void unregister(ServiceInfo serviceInfo) throws Exception;

    /**
     * 关闭与服务注册中心的连接
     */
    void destroy() throws Exception;

}
