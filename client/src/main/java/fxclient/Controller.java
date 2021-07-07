package fxclient;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import utils.DataTypes;
import utils.DirInfo;
import utils.FileInfo;
import utils.FileWorker;


import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller implements Initializable {
    private enum State{
        ACCEPTING_FILE, ACCEPTING_FILES_LIST, AWAIT;
    }
    private State currentState;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ObjectDecoderInputStream odis;
    private byte[] buffer;
    private static final String ADDR = "localhost";
    private static final int PORT = 8189;
    private String currentDirServer;
    private List<FileInfo> filesIncurrentDirServer;
    private FileInfo currentDownloadingFile;


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

    @FXML
    private Button downloadBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentState = State.AWAIT;
        connect();
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
        clientTable.getItems().clear();
        try {

            clientTable.getItems().addAll(Files.list(currentDir).map(FileInfo::new).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error while updating files list");
        }
    }

    public void initServerTable() {
        serverPwd.setText(currentDirServer);

        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameColumn.setPrefWidth(200);

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType()));
        typeColumn.setPrefWidth(50);

        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));

        serverTable.getItems().clear();
        serverTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn);
        if (filesIncurrentDirServer != null) {
            serverTable.getItems().addAll(filesIncurrentDirServer);
        }

    }

    private void connect() {
        try {
            buffer = new byte[1024];
            socket = new Socket(ADDR, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            odis = new ObjectDecoderInputStream(socket.getInputStream());

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        if(currentState == State.AWAIT){
                            byte firstByte = in.readByte();
                            if (firstByte == DataTypes.UI_UPDATE_BY_SERVER_CHANGE.getByte()) {
                                currentState = State.ACCEPTING_FILES_LIST;

                            }
                            if(firstByte == DataTypes.FILE_ACCEPT.getByte()){
                                currentState = State.ACCEPTING_FILE;

                            }
                        }


                        if(currentState == State.ACCEPTING_FILES_LIST){
                            currentDirServer = ((DirInfo) odis.readObject()).getPath();
                            System.out.println(currentDirServer);
                            System.out.println("LIST OF SERVERS FILES");
                            filesIncurrentDirServer = (List<FileInfo>) odis.readObject();
                            System.out.println(filesIncurrentDirServer);
                            System.out.println("List FileInfo from server accepted");
                            updateUI();
                            currentState = State.AWAIT;
                        }
                        if(currentState == State.ACCEPTING_FILE){
                            System.out.println("DOWNLOAD!" + currentDownloadingFile);
                            Path fileFromServer = Paths.get("client", "client_storage", currentDownloadingFile.getFullFileName());
                            FileOutputStream fos = new FileOutputStream(fileFromServer.toFile(), true);
                            FileWorker.bytesToFile(buffer, in, fos, currentDownloadingFile.getSize());
                            currentDownloadingFile = null;
                            currentState = State.AWAIT;
                        }

                    }
                } catch (IOException | ClassNotFoundException e) {
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
                uploadBtn.setDisable(true);
                downloadBtn.setDisable(true);
                uploadBtn.setText(">>>>>>>>");
                new Thread(() -> {
                        List<Byte> someByte = new ArrayList<>();
                        byte[] b = new byte[10];

                    try {
                        out.write(DataTypes.FILE.getByte());
                        out.writeInt(fileToSend.getPath().getFileName().toString().getBytes().length);
                        out.write(fileToSend.getPath().getFileName().toString().getBytes());
                        out.writeLong(fileToSend.getSize());
                        FileWorker.bytesToFile(buffer, fileToSend.getFileInputStream(), out, fileToSend.getSize());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlert("Error while uploading file to server");
                    }
                    uploadBtn.setDisable(false);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            uploadBtn.setText("upload");
                            downloadBtn.setDisable(false);
                        }
                    });
                }).start();


            }
        }

    }


    public void downloadFile(){
        if(serverTable.isFocused()){
            FileInfo fileToDownload = serverTable.getSelectionModel().getSelectedItem();
            if(fileToDownload.getType().equals("DIR")){
                return;
            }
            downloadBtn.setDisable(true);
            downloadBtn.setText("<<<<<<<<");
            try {
                currentDownloadingFile= fileToDownload;
                out.write(DataTypes.FILE_REQUEST.getByte());
                out.write(fileToDownload.getFullFileName().getBytes());

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error while request to download");
            }
            downloadBtn.setDisable(false);
            downloadBtn.setText("download");
        }
    }


    private void updateUI() {

        Path currentDir = Paths.get("client", "client_storage");

        serverTable.getItems().clear();
        serverTable.getItems().addAll(filesIncurrentDirServer);

//        try {
//            clientTable.getItems().clear();
//            clientTable.getItems().addAll(Files.list(currentDir).map(FileInfo::new).collect(Collectors.toList()));
//        } catch (IOException e) {
//            showAlert("Error while updating ui");
//            e.printStackTrace();
//        }

    }

    public void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    public void updateUiByServerChange() {
        try {
            out.write(DataTypes.UI_UPDATE_BY_SERVER_CHANGE.getByte());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Connection problem");
        }
    }

}
