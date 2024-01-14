package com.yjy.rpc.server.transport.netty;

import com.yjy.rpc.server.transport.RpcServer;

public class TestNettyServer {
    /**
     * 服务器只有端口号一个参数，绑定，监听是否有netty发送信息
     * 绑定了各种 handler 如NettyRpcRequestHandler
     * @param args
     */
    public static void main(String[] args) {
        RpcServer rpcServer = new NettyRpcServer();
        rpcServer.start(8880);
    }

}
