package cloud.storage.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.serialization.ObjectEncoder;
import utils.DataTypes;
import utils.DirInfo;
import utils.FileInfo;


import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FirstInHandler extends ChannelInboundHandlerAdapter {

    private Path file;
    private BufferedOutputStream bos;
    private FileInputStream fis;
    private Path serverCurrentDir;
    private long acceptingFileSize;
    private long countAcceptingBytes;
    private int sizeOfNameUploadingFile;

    private State currentState;
    private enum State {
        FILE, FILE_NAME, FILE_SIZE, FILE_ACCEPTING, AWAIT,DOWNLOAD_REQUEST, FILE_DELETE;
    }



    public FirstInHandler() {
        currentState = State.AWAIT;
        serverCurrentDir = Paths.get("server", "server_storage");
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been connected!");
        sendFilesList(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("User has been disconnected");

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(currentState);
        ByteBuf byteBuf = ((ByteBuf) msg);

        //todo fix this patch
        if (currentState != State.AWAIT && countAcceptingBytes == 0L && acceptingFileSize == 0L) {
            logAndSwitchState(State.AWAIT);
        }

        if (currentState == State.AWAIT) {

            countAcceptingBytes = 0L;
            acceptingFileSize = 0L;
            byte firstByte = byteBuf.readByte();

            if (firstByte == DataTypes.FILE.getByte()) {
                logAndSwitchState(State.FILE);
            }

            if (firstByte == DataTypes.FILE_REQUEST.getByte()) {
                logAndSwitchState(State.DOWNLOAD_REQUEST);
            }

            if(firstByte == DataTypes.FILE_DELETE_REQUEST.getByte()){
                logAndSwitchState(State.FILE_DELETE);
            }
        }

        if(currentState == State.FILE_DELETE){
            StringBuilder fileNameToDelete = new StringBuilder();
            ByteBuf response = ctx.alloc().buffer();
            ByteBuf signalByte = ctx.alloc().buffer();

            signalByte.writeByte(DataTypes.FILE_DELETE_RESPONSE.getByte());
            ctx.writeAndFlush(signalByte);

            while (byteBuf.isReadable()) {
                fileNameToDelete.append((char)byteBuf.readByte());
            }
            System.out.println("fileNameToDelete:  " + fileNameToDelete);
            try {
                boolean result = Files.deleteIfExists(serverCurrentDir.resolve(fileNameToDelete.toString()));
                if (result) {
                    response.writeBytes((fileNameToDelete  + " has been deleted").getBytes());
                } else {
                    response.writeBytes(("Something wrong while deleting: " + fileNameToDelete ).getBytes());
                }
                ctx.writeAndFlush(response);

            } catch (IOException e) {
                response.writeBytes(("Something wrong while deleting: " + fileNameToDelete ).getBytes());
                ctx.writeAndFlush(response);

            }
            sendFilesList(ctx);
            logAndSwitchState(State.AWAIT);
        }

        if(currentState == State.DOWNLOAD_REQUEST){
            StringBuilder fileNameToDownload = new StringBuilder();
            while (byteBuf.isReadable()) {
                fileNameToDownload.append((char)byteBuf.readByte());
            }
            System.out.println("fileNameToDownload:  " + fileNameToDownload);
            ByteBuf bufSignalByte = ctx.alloc().buffer();
            bufSignalByte.writeByte((byte) DataTypes.FILE_ACCEPT.getByte());
            ctx.writeAndFlush(bufSignalByte);

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(serverCurrentDir.resolve(fileNameToDownload.toString()).toFile()));
            int readFromFile;
            int count = 0;
            while ((readFromFile = bis.read()) != -1){
               ByteBuf buf = ctx.alloc().buffer();
               buf.writeByte(readFromFile);

               ctx.writeAndFlush(buf);
               count +=1;
            }
            System.out.println("Total send: " + count);
            logAndSwitchState(State.AWAIT);
        }




        //file uploading to the server
        if (currentState == State.FILE) {
            System.out.println("readableBytes: " + byteBuf.readableBytes());
            if (byteBuf.readableBytes() >= 4) {
                sizeOfNameUploadingFile = byteBuf.readInt();
                System.out.println("size of file name: " + sizeOfNameUploadingFile);
                logAndSwitchState(State.FILE_NAME);
            }
        }

        if (currentState == State.FILE_NAME) {
            if (byteBuf.readableBytes() >= sizeOfNameUploadingFile) {
                byte[] nameInBytes = new byte[sizeOfNameUploadingFile];
                byteBuf.readBytes(nameInBytes);
                bos = new BufferedOutputStream(new FileOutputStream(serverCurrentDir.resolve(new String(nameInBytes)).toFile()));
                logAndSwitchState(State.FILE_SIZE);
            }
        }

        if (currentState == State.FILE_SIZE) {
            if (byteBuf.readableBytes() >= 8) {
                acceptingFileSize = byteBuf.readLong();
                logAndSwitchState(State.FILE_ACCEPTING);
            }
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
                    sendFilesList(ctx);
                    break;
                }
            }
        }

        System.out.println("acceptingFileSize: " + acceptingFileSize);
        System.out.println("countAcceptingBytes: " + countAcceptingBytes);
        System.out.println();
        System.out.println();

        if (byteBuf.readableBytes() == 0) {
            byteBuf.release();
        }

    }

    private void sendFilesList(ChannelHandlerContext ctx) throws IOException {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(DataTypes.UI_UPDATE_BY_SERVER_CHANGE.getByte());
        ctx.writeAndFlush(buf);

        ctx.pipeline().addFirst(new ObjectEncoder());

        ctx.writeAndFlush(new DirInfo(serverCurrentDir));

        List<FileInfo> arr = Files.list(serverCurrentDir).map(FileInfo::new).collect(Collectors.toList());

        ctx.writeAndFlush(arr);
        ctx.pipeline().remove(ObjectEncoder.class);
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
