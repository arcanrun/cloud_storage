package withoutclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

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
            out.write("HELLO! I AM CLIENT!".getBytes());
            byte[] dataFromServer = new byte[1024];

            int read = in.read(dataFromServer);
            while (read != -1) {
                System.out.println(read);
                for (byte b : dataFromServer) {
                    System.out.print((char)b);
                }
                read = in.read(dataFromServer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
