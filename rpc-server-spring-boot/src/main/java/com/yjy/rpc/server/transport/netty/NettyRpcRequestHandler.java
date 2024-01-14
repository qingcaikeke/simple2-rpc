package com.yjy.rpc.server.transport.netty;

import com.yjy.rpc.core.common.RpcRequest;
import com.yjy.rpc.core.common.RpcResponse;
import com.yjy.rpc.core.exception.RpcException;
import com.yjy.rpc.core.protocol.constant.ProtocolConstants;
import com.yjy.rpc.core.protocol.enums.MessageStatus;
import com.yjy.rpc.core.protocol.enums.MessageType;
import com.yjy.rpc.core.factory.SingletonFactory;
import com.yjy.rpc.core.protocol.MessageHeader;
import com.yjy.rpc.core.protocol.RpcMessage;
import com.yjy.rpc.server.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Slf4j
public class NettyRpcRequestHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final ThreadPoolExecutor threadPoll = new ThreadPoolExecutor(10,10,60L, TimeUnit.SECONDS,new ArrayBlockingQueue<>(1000));
    //将rpcMessage转为rpcRequest交由RpcRequestHandler处理
    private final RpcRequestHandler rpcRequestHandler;
    public NettyRpcRequestHandler(){
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        threadPoll.submit(() -> {
            try {
                RpcMessage responseRpcMessage = new RpcMessage();
                MessageHeader header = msg.getHeader();
                MessageType type = MessageType.parseByType(header.getMessageType());
                log.debug("The message received by the server is: {}", msg.getBody());
                //如果收到的是心跳请求，修改响应头为response，消息体为pong
                if(type == MessageType.HEARTBEAT_REQUEST){
                    header.setMessageType(MessageType.HEARTBEAT_RESPONSE.getType());
                    header.setMessageStatus(MessageStatus.SUCCESS.getCode());
                    responseRpcMessage.setHeader(header);
                    responseRpcMessage.setBody(ProtocolConstants.PONG);
                }else {// 处理 Rpc 请求信息
                    RpcRequest request = (RpcRequest) msg.getBody();
                    RpcResponse response = new RpcResponse();
                    // 设置头部消息类型为rpcResponse
                    header.setMessageType(MessageType.RESPONSE.getType());
                    // 反射调用
                    try {
                        Object result = rpcRequestHandler.handleRpcRequest(request);
                        response.setReturnValue(request);
                        header.setMessageStatus(MessageStatus.SUCCESS.getCode());
                    } catch (Exception e) {
                        log.error("The service [{}], the method [{}] invoke failed!", request.getServiceName(), request.getMethod());
                        // 若不设置，堆栈信息过多，导致报错
                        response.setExceptionValue(new RpcException("Error in remote procedure call, " + e.getMessage()));
                        header.setMessageStatus(MessageStatus.FAIL.getCode());
                    }
                    // 设置响应头部信息
                    responseRpcMessage.setHeader(header);
                    responseRpcMessage.setBody(response);
                }
                log.debug("responseRpcMessage: {}.", responseRpcMessage);
                // 将结果写入，传递到下一个处理器，上一个事件（线程池那个）执行完了，执行下一个事件？
                /**
                 * 0.ctx ：channelHandlerContext，提供了对 Channel、ChannelPipeline 和 ChannelHandler 的操作和访问
                 * 0.1 ：channel类似一个套接字，提供了网络通信的基本操作，比如读取、写入、连接和关闭
                 * 0.2：ChannelPipeline 是一个用于处理和拦截 Channel 传入和传出数据的处理器链
                 * 由一系列的 ChannelHandler 组成，每个 Handler 负责处理特定的类型的事件。当数据通过 Channel 时，它会被传递到 ChannelPipeline 中，经过一系列的处理器处理，最终到达目的地
                 * 1.writeAndFlush：写入并刷新，保证实时性，确保数据尽快到达客户端
                 * 1.1 write 方法用于将数据写入通道的缓冲区，但它并不立即将数据发送到实际的对端
                 * 1.2 flush 立即将数据发送出去
                 * 2.添加一个监听器。如果写操作失败（比如发生异常），该监听器将关闭通道
                 */
                ctx.writeAndFlush(responseRpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } finally {
            // 确保 ByteBuf 被释放，防止发生内存泄露
            ReferenceCountUtil.release(msg);
            }
        });
    }
    /**
     * 用户自定义事件，当触发读空闲时，自动关闭【客户端channel】连接
     *userEventTriggered 方法是 ChannelHandler 接口的一个回调方法，
     * 用于处理用户自定义的事件。在这里，它处理 IdleStateEvent 空闲状态事件。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 如果事件是空闲状态事件
            IdleState state = ((IdleStateEvent) evt).state();
            // 如果是读空闲状态，则输出警告日志并关闭连接
            if (state == IdleState.READER_IDLE) {
                log.warn("idle check happen, so close the connection.");
                ctx.close();
            }
        } else {
            // 如果不是空闲状态事件，则调用父类的方法继续传递事件
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }

}
