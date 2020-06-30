package cloud.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class StringToByteHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        byte[] dataBytes = ((String)msg).getBytes();
        ByteBuf data = ctx.alloc().buffer(dataBytes.length);
        data.writeBytes(dataBytes);
        ctx.writeAndFlush(data);


    }
}