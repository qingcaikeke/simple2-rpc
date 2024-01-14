package com.yjy.rpc.client.transport.netty;

import com.yjy.rpc.core.common.RpcResponse;
import com.yjy.rpc.core.protocol.MessageHeader;
import com.yjy.rpc.core.protocol.RpcMessage;
import com.yjy.rpc.core.protocol.constant.ProtocolConstants;
import com.yjy.rpc.core.protocol.enums.MessageType;
import com.yjy.rpc.core.protocol.enums.SerializationType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NettyRpcResponseHandler  extends SimpleChannelInboundHandler<RpcMessage> {

    /**
     * 存放未处理的响应请求
     */
    public static final Map<Integer, Promise<RpcMessage>> UNPROCESSED_RPC_RESPONSES = new ConcurrentHashMap<>();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        try{
            MessageType type = MessageType.parseByType(msg.getHeader().getMessageType());
            // 如果是 RpcRequest 请求
            if (type == MessageType.RESPONSE) {
                int sequenceId = msg.getHeader().getSequenceId();
                // 拿到还未执行完成的 promise 对象
                Promise<RpcMessage> promise = UNPROCESSED_RPC_RESPONSES.remove(sequenceId);
                Exception exception = ((RpcResponse) msg.getBody()).getExceptionValue();
                if (exception == null) {
                    promise.setSuccess(msg);
                } else {
                    promise.setFailure(exception);
                }
            }
            else if (type == MessageType.HEARTBEAT_RESPONSE) { // 如果是心跳检查请求
                log.debug("Heartbeat info {}.", msg.getBody());
            }

        }finally {
            // 释放内存，防止内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 用户自定义事件处理器，处理写空闲，当检测到写空闲发生自动发送一个心跳检测数据包
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE) {
                log.warn("Write idle happen [{}].", ctx.channel().remoteAddress());
                // 长时间没有发送rpc请求，发一个心跳，保持连接
                RpcMessage rpcMessage = new RpcMessage();
                MessageHeader.builder().serializerType(SerializationType.KRYO.getType());
                MessageHeader header = MessageHeader.build(SerializationType.KRYO.name());
                header.setMessageType(MessageType.HEARTBEAT_REQUEST.getType());
                rpcMessage.setHeader(header);
                rpcMessage.setBody(ProtocolConstants.PING);
                // 发送心跳检测请求
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            // 如果不是空闲状态事件，则调用父类的方法继续传递事件
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * Called when an exception occurs in processing a client message
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
