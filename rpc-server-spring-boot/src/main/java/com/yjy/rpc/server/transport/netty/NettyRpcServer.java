package com.yjy.rpc.server.transport.netty;

import com.yjy.rpc.core.codec.RpcFrameDecoder;
import com.yjy.rpc.core.codec.SharableRpcMessageCodec;
import com.yjy.rpc.server.transport.RpcServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jdk.nashorn.internal.runtime.regexp.JoniRegExp;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Netty 实现的 RpcServer 服务类
 * 客户端向某一ip端口（服务发现得到）发送请求，服务端接收请求（netty监听指定端口），
 * 根据请求信息，找到方法，调用方法，处理，返回
 */
@Slf4j
public class NettyRpcServer implements RpcServer {
    @Override
    public void start(Integer port) {
        //用于处理事件循环的线程池, boss 处理 accept 事件,parent
        EventLoopGroup boss = new NioEventLoopGroup();
        // worker 处理 read/write 事件,child
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            //Netty 的引导类，用于设置服务器的启动参数,strap:带子
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss,worker)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    //使用 ChannelInitializer 来初始化新的连接。对于每个新的连接，会添加一些处理器到连接的 ChannelPipeline
                    //IdleStateHandler：用于处理空闲状态的事件，当连接超过30秒没有收到客户端请求时，会触发 IdleState#READER_IDLE 事件。
                    //RpcFrameDecoder：自定义的 RPC 消息帧解码器。
                    //SharableRpcMessageCodec：自定义的消息编解码器。
                    //NettyRpcRequestHandler：处理 RPC 请求的自定义处理器。
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 30s内没有收到客户端的请求就关闭连接，会触发一个 IdleState#READER_IDLE 事件
                            ch.pipeline().addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new RpcFrameDecoder());
                            ch.pipeline().addLast(new SharableRpcMessageCodec());
                            ch.pipeline().addLast(new NettyRpcRequestHandler());
                        }
                    });
            // 绑定netty端口，同步等待绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(inetAddress, port).sync();
            log.debug("Rpc server add {} started on the port {}.", inetAddress, port);
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        }catch  (UnknownHostException | InterruptedException e) {
            log.error("An error occurred while starting the rpc service.", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
