package com.yjy.rpc.client.transport;

import com.yjy.rpc.core.protocol.RpcMessage;
/**
 * Rpc 客户端类，负责向服务端发起请求（远程过程调用）
 * 接收RequestMetadata（message+地址）得到message
 */
public interface RpcClient {
    /**
     * 发起远程过程调用
     */
    RpcMessage sendRpcRequest(RequestMetadata requestMetadata);
}
