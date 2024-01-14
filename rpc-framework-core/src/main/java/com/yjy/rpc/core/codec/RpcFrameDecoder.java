package com.yjy.rpc.core.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
/**
 * 粘包拆包编码器，使用固定长度的帧解码器，通过约定用定长字节表示接下来数据的长度。<p>
 * 消息头12字节+4字节（消息长度），消息体根据包头的消息长度，解决沾包
 * 非共享，保存了 ByteBuf 的状态信息
 */
public class RpcFrameDecoder extends LengthFieldBasedFrameDecoder {
    /**
     * 得到当前约定协议的帧解码器，
     * <pre>{@code
     *    this.RpcFrameDecoder(1024, 12, 4)
     * }</pre>
     * 引用：{@link RpcFrameDecoder#RpcFrameDecoder(int, int, int)}
     */
    public RpcFrameDecoder() {
        this(1024, 12, 4);
    }

    /**
     * 构造方法
     *
     * @param maxFrameLength    数据帧的最大长度
     * @param lengthFieldOffset 长度域的偏移字节数
     * @param lengthFieldLength 长度域所占的字节数
     */
    public RpcFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}
