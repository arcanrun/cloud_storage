package cloud.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstInHandler extends ChannelInboundHandlerAdapter {
    Path file;
    FileOutputStream fos;
    int interation;

    public FirstInHandler() {
        file = Paths.get("server", "server_storage", "from_client6.txt");
        try {
            fos = new FileOutputStream(file.toFile(), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been connected!");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been disconnected");
        System.out.println("====> " + interation);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf byteBuf = ((ByteBuf) msg);

        while (byteBuf.readableBytes() > 0) {
           fos.write((char)byteBuf.readByte());
        }
        byteBuf.release();


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
