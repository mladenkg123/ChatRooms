package rs.raf.pds.v4.z5;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import rs.raf.pds.v4.z5.messages.ChatMessage;

import java.io.IOException;
import java.util.List;

public class Main extends Application {

    private ChatClient chatClient;
    private TextArea chatArea;
    private TextField inputField;
    private ChatServer chatServer;
    volatile boolean running = true;
    
    
    @Override
    public void start(Stage primaryStage) {
        // Create UI components
        BorderPane root = new BorderPane();
        chatArea = new TextArea();
        inputField = new TextField();
        Button sendButton = new Button("Send");

        // Set up the layout
        root.setCenter(chatArea);
        root.setBottom(inputField);
        root.setRight(sendButton);

        String hostName = getParameters().getRaw().get(0);
        int portNumber = Integer.parseInt(getParameters().getRaw().get(1));
        String userName = getParameters().getRaw().get(2);

        chatClient = new ChatClient(hostName, portNumber, userName);
        chatClient.setChatArea(chatArea);
        chatServer = new ChatServer(portNumber);

        // Set up the action for the send button
        sendButton.setOnAction(event -> sendMessage());

        // Create the scene and set it to the stage
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client");
        primaryStage.setOnCloseRequest(event -> stopChatClient());
        primaryStage.show();

        // Display the message history
        displayMessageHistory();

     
        new Thread(() -> {
            try {	
                chatClient.start();
                while (running) {
                    displayMessageHistory();
                    Thread.sleep(1000); // Adjust the sleep duration as needed
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            if (message.startsWith("/private")) {
                chatClient.sendPrivateMessage(message);
            } else if (message.startsWith("/create")) {
                chatClient.createChatRoom(message);
            } else if (message.startsWith("/invite")) {
                chatClient.sendRoomInvitation(message);
            } else {
                chatClient.sendMessage(message);
            }
            inputField.clear();
        }
    }

    private void displayMessageHistory() {
        List<ChatMessage> messageHistory = chatServer.getMessageHistory();
        Platform.runLater(() -> {
            for (ChatMessage message : messageHistory) {
                chatArea.appendText(message.getUser() + ": " + message.getTxt() + "\n");
            }
        });
    }

    private void stopChatClient() {
        chatClient.stop();
        chatServer.stop();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
