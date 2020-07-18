package fxclient;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import utils.*;


import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    private enum State {
        AWAIT_RESPONSE_ON_READY_TO_UPLOAD, ACCEPTING_FILE, ACCEPTING_FILES_LIST, FILE_UPLOADING, AWAIT_RESPONSE_ON_DELETE_FILE, AWAIT;
    }

    private FileInfo fileToSend;
    private Path currentDir;
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

    @FXML
    private Button upBtnClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentState = State.AWAIT;
        connect();
        initClientTable();

        initServerTable();
    }

    private void initClientTable() {

        currentDir = Paths.get("client", "client_storage").toAbsolutePath();
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
            showAlert("Error while updating files list", "warning");
        }

        clientTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileInfo clickedItem = clientTable.getSelectionModel().getSelectedItem();
                if (clickedItem.getType().equals("DIR")) {
                    currentDir = currentDir.resolve(clickedItem.getName());
                    updateUIClientTable();
                }
            }
        });
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
                        if (currentState == State.AWAIT) {
                            System.out.println("CURRENT STATE: " + currentState);
                            byte firstByte = in.readByte();
                            System.out.println("SIGNAL BYTE: " + firstByte);

                            if (firstByte == DataTypes.UI_UPDATE_BY_SERVER_CHANGE.getByte()) {
                                logAndSwitchState(State.ACCEPTING_FILES_LIST);

                            }
                            if (firstByte == DataTypes.FILE_ACCEPT.getByte()) {
                                logAndSwitchState(State.ACCEPTING_FILE);

                            }
                            if (firstByte == DataTypes.FILE_DELETE_RESPONSE.getByte()) {
                                logAndSwitchState(State.AWAIT_RESPONSE_ON_DELETE_FILE);

                            }
                            if (firstByte == DataTypes.FILE_READY_TO_ACCEPT.getByte()) {
                                logAndSwitchState(State.FILE_UPLOADING);
                            }
                        }

                        if (currentState == State.FILE_UPLOADING) {
                            try {
                                out.writeInt(fileToSend.getPath().getFileName().toString().getBytes().length);
                                out.write(fileToSend.getPath().getFileName().toString().getBytes());
                                out.writeLong(fileToSend.getSize());
                                FileWorker.bytesToFile(buffer, fileToSend.getFileInputStream(), out, fileToSend.getSize());
                            } catch (IOException e) {
                                e.printStackTrace();
                                showAlert("Error while uploading file to server", "warning");
                            }
                            uploadBtn.setDisable(false);
                            Platform.runLater(() -> {
                                uploadBtn.setText("upload");
                                downloadBtn.setDisable(false);
                                logAndSwitchState(State.AWAIT);
                            });
                        }

                        if (currentState == State.ACCEPTING_FILES_LIST) {
                            currentDirServer = ((DirInfo) odis.readObject()).getPath();
                            System.out.println(currentDirServer);
                            System.out.println("LIST OF SERVERS FILES");
                            filesIncurrentDirServer = (List<FileInfo>) odis.readObject();
                            System.out.println(filesIncurrentDirServer);
                            System.out.println("List FileInfo from server accepted");
                            updateUIServerTable();
                            logAndSwitchState(State.AWAIT);

                        }
                        if (currentState == State.ACCEPTING_FILE) {
                            System.out.println("Accepting file-->" + currentDownloadingFile);
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(currentDir.resolve(currentDownloadingFile.getFullFileName()).toFile()));
                            int countAcceptingBytes = 0;
                            while (true) {
                                bos.write(in.read());
                                countAcceptingBytes++;
                                if (currentDownloadingFile.getSize() == countAcceptingBytes) {
                                    System.out.println("File has been accepted : " + countAcceptingBytes);
                                    bos.close();
                                    break;
                                }
                            }
                            Platform.runLater(() -> {
                                downloadBtn.setDisable(false);
                                downloadBtn.setText("download");
                                updateUIClientTable();
                            });
                            logAndSwitchState(State.AWAIT);

                        }
                        if (currentState == State.AWAIT_RESPONSE_ON_DELETE_FILE) {
                            ResultMessageOnDelete response = (ResultMessageOnDelete) odis.readObject();
                            Platform.runLater(() -> {
                                showAlert(response.getMessage(), response.getType());
                            });
                            logAndSwitchState(State.AWAIT);

                        }

                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    try {
                        in.close();
                        out.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        showAlert("Error while close connections", "warning");
                    }

                }


            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error while connecting to server", "warning");

        }
    }

    public void uploadFileToServer() {
        if (clientTable.isFocused()) {
            fileToSend = clientTable.getSelectionModel().getSelectedItem();
            if (!fileToSend.getType().equals("DIR")) {

                uploadBtn.setDisable(true);
                downloadBtn.setDisable(true);
                uploadBtn.setText(">>>>>>>>");

                try {
                    out.write(DataTypes.FILE.getByte());
                } catch (IOException e) {
                    showAlert("Something wrong in response to file uploading", "warining");
                }

            }
        }

    }

    public void downloadFile() {
        if (serverTable.isFocused()) {
            FileInfo fileToDownload = serverTable.getSelectionModel().getSelectedItem();
            if (fileToDownload.getType().equals("DIR")) {
                return;
            }
            downloadBtn.setDisable(true);
            downloadBtn.setText("<<<<<<<<");
            try {
                currentDownloadingFile = fileToDownload;
                out.write(DataTypes.FILE_REQUEST.getByte());
                out.write(fileToDownload.getFullFileName().getBytes());


            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error while request to download", "warning");
            }

        }
    }

    private void updateUIServerTable() {

        serverPwd.setText(currentDirServer);

        serverTable.getItems().clear();
        serverTable.getItems().addAll(filesIncurrentDirServer);

    }

    private void updateUIClientTable() {
        pwd.setText(currentDir.toString());
        try {
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(currentDir).map(FileInfo::new).collect(Collectors.toList()));
        } catch (IOException e) {
            showAlert("Error while updating ui", "warning");
            e.printStackTrace();
        }
    }

    public void showAlert(String msg, String typeAlert) {
        Alert alert;
        if (typeAlert.equals("info")) {
            alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        } else {
            alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        }
        alert.showAndWait();
    }

    public void deleteFileOrDir() {
        if (clientTable.isFocused()) {
            FileInfo fileToDelete = clientTable.getSelectionModel().getSelectedItem();
            String FileOrDir = fileToDelete.getType().equals("DIR") ? "Directory" : "File";

            try {
                boolean result = Files.deleteIfExists(fileToDelete.getPath());
                if (result) {
                    showAlert(FileOrDir + ": " + fileToDelete.getFullFileName() + "has been deleted", "info");
                } else {
                    showAlert("Something wrong while deleting: " + FileOrDir + " " + fileToDelete.getFullFileName(), "warning");
                }
                updateUIClientTable();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(e.toString(), "warning");

            }

        }
        if (serverTable.isFocused()) {
            FileInfo fileToDelete = serverTable.getSelectionModel().getSelectedItem();
            try {
                out.write(DataTypes.FILE_DELETE_REQUEST.getByte());
                out.write(fileToDelete.getFullFileName().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void logAndSwitchState(State newState) {

        System.out.println("STATE SWITCHED FROM [" + currentState + "] TO [" + newState + "]");
        currentState = newState;
    }

    public void goTo(ActionEvent e) {
        Object src = e.getSource();
        if (src == upBtnClient) {
            Path parent = currentDir.getParent();
            if (parent != null) {
                currentDir = currentDir.getParent();
                updateUIClientTable();
            }
        }

    }


}
