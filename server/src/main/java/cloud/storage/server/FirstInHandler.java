package cloud.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstInHandler extends ChannelInboundHandlerAdapter {
    Path file;
    FileOutputStream fos;
    FileInputStream fis;


    public FirstInHandler() {
        file = Paths.get("server", "server_storage", "img.jpg");
        try {
            fos = new FileOutputStream(file.toFile(), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been connected!");
        Path file = Paths.get("server", "server_storage", "from_server.txt");
        fis = new FileInputStream(file.toFile());


        byte[] buffer = new byte[256];
        ByteBuf data = ctx.alloc().buffer(buffer.length);



        int count = 0;
        long fileSize = (file.toFile().length());

        if (fileSize < buffer.length) {
            byte[] lessBuffer = new byte[(int) fileSize];
            int read = fis.read(lessBuffer);
            count += read;

            ByteBuf lessData = ctx.alloc().buffer(lessBuffer.length);
            lessData.writeBytes(lessBuffer);
            ctx.writeAndFlush(lessData);

        } else {
            while (true) {
                int read = fis.read(buffer);
                count += read;


//                data.writeBytes(buffer);
                ctx.writeAndFlush(Unpooled.copiedBuffer(buffer));

                if ((fileSize - count) < buffer.length) {
                    byte[] leftBuffer = new byte[(int) (fileSize - count)];
                    read = fis.read(leftBuffer);
                    count += read;

                    ByteBuf leftData = ctx.alloc().buffer(leftBuffer.length);
                    leftData.writeBytes(leftBuffer);
                    ctx.writeAndFlush(leftData);

                    break;
                }

            }
        }


        System.out.println();
        System.out.println("READ: " + count);




    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been disconnected");

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf byteBuf = ((ByteBuf) msg);

        while (byteBuf.readableBytes() > 0) {
            fos.write((char) byteBuf.readByte());
        }
        byteBuf.release();


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
