package fxclient;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import utils.FileInfo;
import utils.FileWorker;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private byte[] buffer;
    private static final String ADDR = "localhost";
    private static final int PORT = 8189;

    private enum DataTypes {
        FILE((byte) 15), SERVER_ERROR((byte) 29);
        byte signalByte;

        DataTypes(byte signalByte) {
            this.signalByte = signalByte;
        }

        byte getSignalByte() {
            return signalByte;
        }
    }

    @FXML
    private TextField pwd;

    @FXML
    private TextField serverPwd;

    @FXML
    private TableView<FileInfo> clientTable;

    @FXML
    private TableView<FileInfo> serverTable;

    @FXML
    private Button uploadBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        connect();

        ObjectDecoderInputStream odis = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
        FileInfo h = null;
        try {
            h = ((FileInfo) odis.readObject());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("FileInfo from server"+h);
        System.out.println("NAME: "+h.getName());
        initClientTable();
        initServerTable();

    }

    private void initClientTable() {
        Path currentDir = Paths.get("client", "client_storage");
        pwd.setText(currentDir.toString());

        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameColumn.setPrefWidth(200);

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType()));
        typeColumn.setPrefWidth(50);

        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));


        clientTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn);
        try {

            clientTable.getItems().addAll(Files.list(currentDir).map(FileInfo::new).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error while updating files list");
        }
    }

    public void initServerTable() {
        // from server
//        Path currentDir = Paths.get("client", "client_storage");
//        serverPwd.setText(currentDir.toString());

        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameColumn.setPrefWidth(200);

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType()));
        typeColumn.setPrefWidth(50);

        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));


//        clientTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn);
//        try {
//            clientTable.getItems().addAll(Files.list(currentDir).map(FileInfo::new).collect(Collectors.toList()));
//        } catch (IOException e) {
//            e.printStackTrace();
//            showAlert("Error while updating files list");
//        }
    }

    private void connect() {
        try {
            buffer = new byte[1024];
            socket = new Socket(ADDR, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());


            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        byte firstByte = in.readByte();
                        if (firstByte == (byte) 25) {
                            System.out.println("LIST OF SERVERS FILES");
                            System.out.println("0----");
                        }


                    }
                } catch (IOException ) {
                    e.printStackTrace();
                    try {
                        in.close();
                        out.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();

                        showAlert("Error while close connections");


                    }

                }


            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error while connecting to server");

        }
    }

    public void uploadFileToServer() {

        if (clientTable.isFocused()) {
            FileInfo fileToSend = clientTable.getSelectionModel().getSelectedItem();


            if (!fileToSend.getType().equals("DIR")) {
                try {
                    out.write(DataTypes.FILE.getSignalByte());
                    out.writeInt(fileToSend.getPath().getFileName().toString().getBytes().length);
                    out.write(fileToSend.getPath().getFileName().toString().getBytes());
                    out.writeLong(fileToSend.getSize());
                    FileWorker.bytesToFile(buffer, fileToSend.getFileInputStream(), this.out, fileToSend.getPath(), fileToSend.getSize());
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error while uploading file to server");
                }
            }
        }
//        updateView();
    }

    private void updateView() {
        throw new RuntimeException("NOT YET IMPL");
    }

    public void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }


}
