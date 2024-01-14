package com.yjy.rpc.core.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.buffer.ByteBuf;
/**
 * 改用SharableRpcMessageCodec
 */
public class RpcMessageEncoder<T> extends MessageToByteEncoder<T> {

    @Override
    protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
        // todo: implement this method.
    }
}
