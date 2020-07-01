package cloud.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import utils.FileInfo;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstInHandler extends ChannelInboundHandlerAdapter {
    private Path file;
    private BufferedOutputStream bos;
    private FileInputStream fis;
    private Path serverRootPath;
    private long acceptingFileSize;
    private long countAcceptingBytes;

    private enum State {
        FILE, FILE_NAME, FILE_SIZE, FILE_ACCEPTING, AWAIT;
    }

    private State currentState;

    public FirstInHandler() {
        currentState = State.AWAIT;
        serverRootPath = Paths.get("server", "server_storage");
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        System.out.println("User has been connected!");


        buf.writeByte((byte)25);
        ctx.writeAndFlush(buf);

        FileInfo fi = new FileInfo(serverRootPath.resolve("from_client_min.txt"));

        System.out.println(fi);
        ctx.writeAndFlush(fi);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been disconnected");

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(currentState);

        ByteBuf byteBuf = ((ByteBuf) msg);

        if (currentState == State.AWAIT) {
            countAcceptingBytes = 0L;
            if (byteBuf.readByte() == (byte) 15) {
                logAndSwitchState(State.FILE);
            }

        }
        if (currentState == State.FILE) {

                int sizeOfName = byteBuf.readInt();
                System.out.println("length of file name: " + sizeOfName);
                byte[] nameInBytes = new byte[sizeOfName];
                byteBuf.readBytes(nameInBytes);
                bos = new BufferedOutputStream(new FileOutputStream(serverRootPath.resolve(new String(nameInBytes)).toFile()));
                logAndSwitchState(State.FILE_SIZE);

        }
        if (currentState == State.FILE_SIZE) {
            acceptingFileSize = byteBuf.readLong();
            logAndSwitchState(State.FILE_ACCEPTING);

        }
        if (currentState == State.FILE_ACCEPTING) {
            System.out.println("READBLE BYTES: " + byteBuf.readableBytes());
            while (byteBuf.readableBytes() > 0) {
                bos.write(byteBuf.readByte());
                countAcceptingBytes++;
                if (acceptingFileSize == countAcceptingBytes) {
                    System.out.println("File has been accepted : " + countAcceptingBytes);
                    logAndSwitchState(State.AWAIT);
                    bos.close();
                    break;
                }

            }
        }
        System.out.println();
        System.out.println();
        System.out.println(acceptingFileSize);
        System.out.println(countAcceptingBytes);

        if (byteBuf.readableBytes() == 0) {
            byteBuf.release();
        }


    }

    private void logAndSwitchState(State newState) {

        System.out.println("STATE SWITCHED FROM [" + currentState + "] TO [" + newState + "]");
        currentState = newState;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
