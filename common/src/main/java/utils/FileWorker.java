package utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
public class FileWorker {


            //accept from server
//            Path fileFromServer = Paths.get("client", "client_storage", "from_server.txt");
//            FileOutputStream fos = new FileOutputStream(fileFromServer.toFile(), true);
//            int fileSize = 1099;
//            bytesToFile(buffer, in, fos,  fileSize);




            //send to server
//             Path file = Paths.get("client", "client_storage", "img2.png");
//             FileInputStream fis = new FileInputStream(file.toFile());
//             long fileToServerSize = (file.toFile().length());
//             bytesToFile(buffer, fis, out, fileToServerSize);
//            in.close();
//            out.close();
//            System.out.println("END CLIENT");


    public static void bytesToFile(byte[] buffer, InputStream in, OutputStream out, long fileSize) throws IOException {

        int count = 0;

            if (fileSize < buffer.length) {
                byte[] lessBuffer = new byte[(int) fileSize];
                int read = in.read(lessBuffer);
                count += read;
                out.write(lessBuffer);
                logging(count);
                return;
            } else {
                while (true) {
                    int read = in.read(buffer);
                    count += read;
                    out.write(buffer);
                    if ((fileSize - count) < buffer.length) {
                        byte[] leftBuffer = new byte[(int) (fileSize - count)];
                        read = in.read(leftBuffer);
                        count += read;
                        out.write(leftBuffer);
                        logging(count);
                        return;
                    }
                }
            }

    }

    private static void logging(int readBytes){
        System.out.println();
        System.out.println("TOTAL READ: " + readBytes);
    }

}
