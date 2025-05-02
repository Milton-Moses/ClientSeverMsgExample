package org.example.clientsevermsgexample;



import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import static java.lang.Thread.sleep;

public class MainController implements Initializable {

    public Button user1_client;
    public Button user2_server;
    private Socket chatSocket;
    private DataInputStream chatDis;
    private DataOutputStream chatDos;
    private boolean isChatRunning = false;
    private Stage chatStage;
    private VBox chatMessageBox;

    @FXML
    private ComboBox dropdownPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7",     // ping
                "13",     // daytime
                "21",     // ftp
                "23",     // telnet
                "71",     // finger
                "80",     // http
                "119",     // nntp (news)
                "161"      // snmp);
        );
    }

    @FXML
    private Button clearBtn;



    @FXML
    private TextArea resultArea;

    @FXML
    private Label server_lbl;

    @FXML
    private Button testBtn;

    @FXML
    private Label test_lbl;

    @FXML
    private TextField urlName;

    Socket socket1;

    Label lb122, lb12;
    TextField msgText;

    @FXML
    void startChatAsUser1(ActionEvent event) {
        startChatWindow("User 1");
        connectAsClient();
    }

    @FXML
    void startChatAsUser2(ActionEvent event) {
        startChatWindow("User 2");
        startChatServer();
    }

    private void startChatWindow(String title) {
        try {
            chatStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chat_form.fxml"));
            Parent root = loader.load();

            // Get UI references
            chatMessageBox = (VBox) root.lookup("#vbox_messages");
            TextField messageField = (TextField) root.lookup("#tf_message");
            Button sendButton = (Button) root.lookup("#button_send");

            // Set up event handlers
            sendButton.setOnAction(e -> sendChatMessage(messageField.getText()));
            messageField.setOnAction(e -> {
                sendChatMessage(messageField.getText());
                messageField.clear();
            });

            Scene scene = new Scene(root);
            chatStage.setScene(scene);
            chatStage.setTitle(title);
            chatStage.setOnCloseRequest(e -> stopChat());
            chatStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectAsClient() {
        new Thread(() -> {
            try {
                chatSocket = new Socket("localhost", 6667);
                setupChatStreams();
                appendMessage("Connected to chat server!");
                listenForMessages();
            } catch (IOException e) {
                appendMessage("Connection error: " + e.getMessage());
            }
        }).start();
    }

    private void startChatServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(6667);
                appendMessage("Waiting for connection...");
                chatSocket = serverSocket.accept();
                serverSocket.close();
                setupChatStreams();
                appendMessage("User connected!");
                listenForMessages();
            } catch (IOException e) {
                appendMessage("Server error: " + e.getMessage());
            }
        }).start();
    }

    private void setupChatStreams() throws IOException {
        chatDis = new DataInputStream(chatSocket.getInputStream());
        chatDos = new DataOutputStream(chatSocket.getOutputStream());
        isChatRunning = true;
    }

    private void sendChatMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;

        try {
            chatDos.writeUTF(message);
            chatDos.flush();
            appendMessage("You: " + message);
        } catch (IOException e) {
            appendMessage("Failed to send message: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            while (isChatRunning) {
                String message = chatDis.readUTF();
                appendMessage("Other: " + message);
            }
        } catch (IOException e) {
            if (isChatRunning) {
                appendMessage("Connection lost: " + e.getMessage());
            }
        }
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-padding: 5px;");
            chatMessageBox.getChildren().add(messageLabel);

            // Auto-scroll to bottom
            chatMessageBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                ((ScrollPane) chatMessageBox.getParent()).setVvalue(1.0);
            });
        });
    }

    private void stopChat() {
        isChatRunning = false;
        try {
            if (chatDis != null) chatDis.close();
            if (chatDos != null) chatDos.close();
            if (chatSocket != null) chatSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void checkConnection(ActionEvent event) {

        String host = urlName.getText();
        int port = Integer.parseInt(dropdownPort.getValue().toString());

        try {
            Socket sock = new Socket(host, port);
            resultArea.appendText(host + " listening on port " + port + "\n");
            sock.close();
        } catch (UnknownHostException e) {
            resultArea.setText(String.valueOf(e) + "\n");
            return;
        } catch (Exception e) {
            resultArea.appendText(host + " not listening on port "
                    + port + "\n");
        }


    }


    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
        urlName.setText("");

    }



    @FXML
    void startServer(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();
        Label lb11 = new Label("Server");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);

        lb12 = new Label("info");
        lb12.setLayoutX(100);
        lb12.setLayoutY(200);
        root.getChildren().addAll(lb11, lb12);
        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        lb12.setText("Server is running and waiting for a client...");

        stage.setTitle("Server");
        stage.show();


        new Thread(this::runServer).start();

    }

    String message;

    private void runServer() {
        try {

            ServerSocket serverSocket = new ServerSocket(6666);
            updateServer("Server is running and waiting for a client...");
            while (true) { // Infinite loop
                try {
                    Socket clientSocket = serverSocket.accept();
                    updateServer("Client connected!");

                    new Thread(() -> {
                        try {
                            sleep(3000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                    message = dis.readUTF();
                    updateServer("Message from client: " + message);

                    // Sending a response back to the client
                    dos.writeUTF("Received: " + message);

                    dis.close();
                    dos.close();

                } catch (IOException e) {
                    updateServer("Error: " + e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (message.equalsIgnoreCase("exit")) break;

            }
        } catch (IOException e) {
            updateServer("Error: " + e.getMessage());
        }
    }

    private void updateServer(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb12.setText(message + "\n"));
    }


    @FXML
    void startClient(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();
        Button connectButton = new Button("Connect to server");
        connectButton.setLayoutX(100);
        connectButton.setLayoutY(300);
        connectButton.setOnAction(this::connectToServer);
        // new Thread(this::connectToServer).start();

        Label lb11 = new Label("Client");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);
        msgText = new TextField("msg");
        msgText.setLayoutX(100);
        msgText.setLayoutY(150);

        lb122 = new Label("info");
        lb122.setLayoutX(100);
        lb122.setLayoutY(200);
        root.getChildren().addAll(lb11, lb122, connectButton, msgText);


        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();


    }


    private void connectToServer(ActionEvent event) {


        try {
            socket1 = new Socket("localhost", 6666);

            DataOutputStream dos = new DataOutputStream(socket1.getOutputStream());
            DataInputStream dis = new DataInputStream(socket1.getInputStream());

            dos.writeUTF(msgText.getText());
            String response = dis.readUTF();
            updateTextClient("Server response: " + response + "\n");

            dis.close();
            dos.close();
            socket1.close();
        } catch (Exception e) {
            updateTextClient("Error: " + e.getMessage() + "\n");
        }


    }

    private void updateTextClient(String message) {
        // Run on the UI thread
        javafx.application.Platform.runLater(() -> lb122.setText(message + "\n"));
    }

}
