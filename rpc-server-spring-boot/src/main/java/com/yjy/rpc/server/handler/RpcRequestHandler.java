package com.yjy.rpc.server.handler;

import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.exception.RpcException;
import com.yjy.rpc.server.serviceCache.LocalServiceCache;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
/**
 * Rpc 请求调用处理器
 * 根据请求，去本地缓存serviceMap中拿到当初注册的实现类对象
 * 反射获得clazz获得方法，调用方法，得到object结果
 */
@Slf4j
public class RpcRequestHandler {

    /**
     * 处理 RpcRequest
     *
     * @param request rpc request 对象
     * @return 返回方法调用结果
     * @throws Exception 反射调用方法失败，抛出异常
     */
    public Object handleRpcRequest(RpcRequest request) throws Exception {
        // 反射调用 RpcRequest 请求指定的方法
        // 获取请求服务实例
        Object service = LocalServiceCache.getService(request.getServiceName());
        if (service == null) {
            log.error("The service [{}] is not exist!", request.getServiceName());
            throw new RpcException(String.format("The service [%s] is not exist!", request.getServiceName()));
        }
        // 获取指定的方法
        Method method = service.getClass().getMethod(request.getMethod(), request.getParameterTypes());
        // 调用方法并返回结果
        return method.invoke(service, request.getParameterValues());
    }

}
