package withoutclient;


import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;

    public static void main(String[] args) {
        socket = null;
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[256];




            Path fileFromServer = Paths.get("client", "client_storage", "from_server.txt");
            FileOutputStream fos = new FileOutputStream(fileFromServer.toFile(), true);

            int readFromServer = in.read(buffer);
            int count = 0;
            while (readFromServer != -1){
                count += readFromServer;
                System.out.println(readFromServer);
                fos.write(buffer);
                if(count == 1099){
                    break;
                }
                readFromServer = in.read(buffer);
            }

            in.close();
            out.close();
            System.out.println("END CLIENT");

        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    private static void sendFileToServer(byte[] buffer) throws IOException {
        Path file = Paths.get("client", "client_storage", "img.jpg");
        FileInputStream fis = new FileInputStream(file.toFile());

        int count = 0;
        long fileSize = (file.toFile().length());

        if (fileSize < buffer.length) {
            byte[] lessBuffer = new byte[(int) fileSize];
            int read = fis.read(lessBuffer);
            count += read;
            out.write(lessBuffer);
        } else {
            while (true) {
                int read = fis.read(buffer);
                count += read;
                out.write(buffer);
                if ((fileSize - count) < buffer.length) {
                    byte[] leftBuffer = new byte[(int) (fileSize - count)];
                    read = fis.read(leftBuffer);
                    count += read;
                    out.write(leftBuffer);
                    break;
                }

            }
        }


        System.out.println();
        System.out.println("READ: " + count);
    }
}
