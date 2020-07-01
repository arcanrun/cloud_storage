package fxclient;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.FileWorker;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private byte[] buffer;
    private static final String ADDR = "localhost";
    private static final int PORT = 8189;


    @FXML
    private TextField pwd;

    @FXML
    private TableView<FileInfo> clientTable;

    @FXML
    private Button uploadBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        connect();
    }

    private void connect() {
        try {
            buffer = new byte[256];
            socket = new Socket(ADDR, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());


            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        int read = in.read(buffer);
                        for (byte b : buffer) {
                            System.out.print((char) b);
                        }
                    }
                } catch (IOException e) {
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
