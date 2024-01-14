package com.yjy.rpc.client.proxy;

import com.yjy.rpc.client.config.RpcClientProperties;
import com.yjy.rpc.client.transport.RequestMetadata;
import com.yjy.rpc.client.transport.RpcClient;
import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.common.RpcResponse;
import com.yjy.rpc.core.common.ServiceInfo;
import com.yjy.rpc.core.discovery.ServiceDiscovery;
import com.yjy.rpc.core.exception.RpcException;
import com.yjy.rpc.core.protocol.MessageHeader;
import com.yjy.rpc.core.protocol.RpcMessage;

import java.lang.reflect.Method;

/**
 * 远程方法调用工具类
 */
public class RemoteMethodCall {
    public static Object remoteCall(ServiceDiscovery discovery, RpcClient rpcClient, String serviceName,
                                    RpcClientProperties properties, Method method, Object[] args) {
        // 构建请求头
        MessageHeader header = MessageHeader.build(properties.getSerialization());

        // 构建请求体
        RpcRequest request = new RpcRequest();
        request.setServiceName(serviceName);
        request.setMethod(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameterValues(args);

        // 构建通信协议信息
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setHeader(header);
        rpcMessage.setBody(request);

        // 进行服务发现
        ServiceInfo serviceInfo = discovery.discover(request);
        if (serviceInfo == null) {
            throw new RpcException(String.format("The service [%s] was not found in the remote registry center.",
                    serviceName));
        }
        // 构建请求元数据
        RequestMetadata metadata = RequestMetadata.builder()
                .rpcMessage(rpcMessage)
                .serverAddr(serviceInfo.getAddress())
                .port(serviceInfo.getPort())
                .timeout(properties.getTimeout()).build();

        // todo：此处可以实现失败重试机制
        // 用rpcClient发送网络请求，获取结果
        RpcMessage responseRpcMessage = rpcClient.sendRpcRequest(metadata);
        if (responseRpcMessage == null) {
            throw new RpcException("Remote procedure call timeout.");
        }

        // 获取响应结果
        RpcResponse response = (RpcResponse) responseRpcMessage.getBody();

        // 如果 远程调用 发生错误
        if (response.getExceptionValue() != null) {
            throw new RpcException(response.getExceptionValue());
        }
        // 返回响应结果
        return response.getReturnValue();




    }

}
