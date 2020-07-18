package cloud.storage.server;

import cloud.storage.server.net.utils.AuthService;
import cloud.storage.server.net.utils.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.serialization.ObjectEncoder;
import utils.DataTypes;
import utils.DirInfo;
import utils.FileInfo;
import utils.ResultMessageOnDelete;


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
    private AuthService authService;
    private User user;

    private State currentState;

    private enum State {
        FILE, FILE_NAME, FILE_SIZE, FILE_ACCEPTING, AWAIT, DOWNLOAD_REQUEST, FILE_DELETE, USER_AUTH;
    }


    public FirstInHandler() {
        authService = new AuthService();
        authService.connect();
        currentState = State.AWAIT;
        serverCurrentDir = Paths.get("server", "server_storage");
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Connection established!");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(user+" has been disconnected");
        ctx.close();

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(currentState);
        ByteBuf byteBuf = ((ByteBuf) msg);


        if (currentState == State.AWAIT) {

            countAcceptingBytes = 0L;
            acceptingFileSize = 0L;
            byte firstByte = byteBuf.readByte();

            if (firstByte == DataTypes.FILE.getByte()) {
                ByteBuf signalByteBuf = ctx.alloc().buffer();
                signalByteBuf.writeByte(DataTypes.FILE_READY_TO_ACCEPT.getByte());
                ctx.writeAndFlush(signalByteBuf);

                logAndSwitchState(State.FILE);
            }

            if (firstByte == DataTypes.FILE_REQUEST.getByte()) {
                logAndSwitchState(State.DOWNLOAD_REQUEST);
            }

            if (firstByte == DataTypes.FILE_DELETE_REQUEST.getByte()) {
                logAndSwitchState(State.FILE_DELETE);
            }
            if (firstByte == DataTypes.AUTH_USER_REQUEST.getByte()){
                logAndSwitchState(State.USER_AUTH);
            }
        }

        if(currentState == State.USER_AUTH){
            StringBuilder loginAndPass = new StringBuilder();
            while (byteBuf.isReadable()){
                loginAndPass.append((char) byteBuf.readByte());
            }
            System.out.println(loginAndPass);   String login = loginAndPass.toString().split("~")[0];
            int password = Integer.parseInt(loginAndPass.toString().split("~")[1]);
            user = authService.loginUserByLoginAndPass(login, password);

            if(user == null){
                System.out.println("ERROR AUTH");
            }else {
                serverCurrentDir = serverCurrentDir.resolve(user.getLogin());
                ByteBuf signalByteBuf =ctx.alloc().buffer();
                signalByteBuf.writeByte(DataTypes.AUTH_OK.getByte());
                ctx.writeAndFlush(signalByteBuf);
                sendFilesList(ctx);
                logAndSwitchState(State.AWAIT);
            }

        }

        if (currentState == State.FILE_DELETE) {
            StringBuilder fileNameToDelete = new StringBuilder();
            ResultMessageOnDelete response = new ResultMessageOnDelete();
            ByteBuf signalByteBuf = ctx.alloc().buffer();

            signalByteBuf.writeByte(DataTypes.FILE_DELETE_RESPONSE.getByte());
            ctx.writeAndFlush(signalByteBuf);

            while (byteBuf.isReadable()) {
                fileNameToDelete.append((char) byteBuf.readByte());
            }
            System.out.println("fileNameToDelete:  " + fileNameToDelete);
            try {
                boolean result = Files.deleteIfExists(serverCurrentDir.resolve(fileNameToDelete.toString()));
                if (result) {
                    response.setMessage(fileNameToDelete + " has been deleted");
                    response.setType("info");
                } else {
                    response.setMessage("Something wrong while deleting: " + fileNameToDelete);
                    response.setType("warning");

                }

            } catch (IOException e) {
                response.setMessage(e.toString());
                response.setType("warning");

            }

            ctx.pipeline().addFirst(new ObjectEncoder());
            ctx.writeAndFlush(response);
            ctx.pipeline().remove(ObjectEncoder.class);

            sendFilesList(ctx);
            logAndSwitchState(State.AWAIT);
        }

        if (currentState == State.DOWNLOAD_REQUEST) {
            StringBuilder fileNameToDownload = new StringBuilder();
            while (byteBuf.isReadable()) {
                fileNameToDownload.append((char) byteBuf.readByte());
            }
            System.out.println("fileNameToDownload:  " + fileNameToDownload);
            ByteBuf bufSignalByte = ctx.alloc().buffer();
            bufSignalByte.writeByte((byte) DataTypes.FILE_ACCEPT.getByte());
            ctx.writeAndFlush(bufSignalByte);

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(serverCurrentDir.resolve(fileNameToDownload.toString()).toFile()));
            int readFromFile;
            int count = 0;
            while ((readFromFile = bis.read()) != -1) {
                ByteBuf buf = ctx.alloc().buffer();
                buf.writeByte(readFromFile);

                ctx.writeAndFlush(buf);
                count += 1;
            }
            System.out.println("Total send: " + count);
            logAndSwitchState(State.AWAIT);
        }

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
        ByteBuf signalByte = ctx.alloc().buffer();
        signalByte.writeByte(DataTypes.UI_UPDATE_BY_SERVER_CHANGE.getByte());
        ctx.writeAndFlush(signalByte);

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
