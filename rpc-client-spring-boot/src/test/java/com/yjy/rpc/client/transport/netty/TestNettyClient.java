package com.yjy.rpc.client.transport.netty;

import com.yjy.rpc.client.transport.RequestMetadata;
import com.yjy.rpc.client.transport.RpcClient;
import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.protocol.MessageHeader;
import com.yjy.rpc.core.protocol.RpcMessage;
import com.yjy.rpc.core.protocol.enums.MessageType;
import com.yjy.rpc.core.protocol.enums.SerializationType;

public class TestNettyClient {
    public static void main(String[] args) {
        RpcClient rpcClient = new NettyRpcClient();
        RpcMessage rpcMessage = new RpcMessage();

        //传入序列化方式构建，其他参数自定义，也可以后续使用build构建
        MessageHeader header = MessageHeader.build(SerializationType.KRYO.name());
        header.setMessageType(MessageType.REQUEST.getType());
        rpcMessage.setHeader(header);

        RpcRequest request = new RpcRequest();
        rpcMessage.setBody(request);

        RequestMetadata metadata = RequestMetadata.builder()
                .rpcMessage(rpcMessage)
                //正常需要使用serviceDiscovery进行服务发现，得到ServiceInfo
                .serverAddr("192.168.0.5")
                .port(8880).build();
        //序列号在发送时添加，建立netty连接，发送
        rpcClient.sendRpcRequest(metadata);
    }
}
