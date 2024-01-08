package rs.raf.pds.v4.z5;

import javafx.application.Application;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Main extends Application {

    private ChatClient chatClient;
    private TextArea chatArea;
    private TextField inputField;
    private ChatServer chatServer;
    volatile boolean running = true;
    private ListView<ChatMessage> chatListView = new ListView<>();
    Stage primaryStage;
    Alert alert = new Alert(Alert.AlertType.WARNING);
    String userName;
    String host = "localhost";
    int port = 9000;

   
    
    @Override
    public void start(Stage primaryStage) {
    	
    	this.primaryStage = primaryStage;
    	
    	BorderPane rootUserGUI = new BorderPane();
        TextField inputUsernameField = new TextField();
        Button joinButton = new Button("Join");

          
        rootUserGUI.setCenter(inputUsernameField);
        rootUserGUI.setBottom(joinButton);

       
        Scene usernameSelectionScene = new Scene(rootUserGUI, 300, 150);
        primaryStage.setScene(usernameSelectionScene);
        primaryStage.setTitle("Username Selection");
        primaryStage.show();

        joinButton.setOnAction(event -> {
            userName = inputUsernameField.getText().trim();
            if (!userName.isEmpty()) {
                
                primaryStage.close();
                startChatClient();
            } else {
            	alert.setTitle("Warning");
            	alert.setHeaderText(null);
            	alert.setContentText("Username cannot be empty!");
            	alert.showAndWait();
            }
        });
    }
    	
    	
    private void startChatClient() {
        BorderPane root = new BorderPane();
        chatArea = new TextArea();
        inputField = new TextField();
        Button sendButton = new Button("Send");

        root.setStyle("-fx-background-color: black; -fx-text-fill: white;");
        chatListView.setStyle("-fx-control-inner-background: black;");
        inputField.setStyle("-fx-control-inner-background: black; -fx-text-fill: white;");
        sendButton.setStyle("-fx-background-color: darkgrey; -fx-text-fill: black;");

        root.setCenter(chatListView);
        root.setBottom(inputField);
        root.setRight(sendButton);

        
        chatClient = new ChatClient(host, port, userName);
        chatClient.setChatArea(chatArea);
        chatServer = new ChatServer(port);

        sendButton.setOnAction(event -> sendMessage());

        inputField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                sendMessage();
            }
        });

        setContextMenu();

        Scene chatScene = new Scene(root, 600, 450);
        primaryStage.setScene(chatScene);
        primaryStage.setTitle("Chat Client");
        primaryStage.setOnCloseRequest(event -> stopChatClient());
        primaryStage.show();

        displayMessageHistory();

        chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateContextMenu(newValue);
            }
        });

        new Thread(() -> {
            try {
                chatClient.start();
                while (running) {
                    displayMessageHistory();
                    Thread.sleep(3000); 
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
    
    
    private void setContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        MenuItem replyItem = new MenuItem("Reply");
        editItem.setOnAction(event -> {
        	
             handleEditAction();
        });
        
        replyItem.setOnAction(event -> {
       
        	handleReplyAction();
        	
        });

        contextMenu.getItems().add(editItem);
        contextMenu.getItems().add(replyItem);

        // Set the new context menu to the ListView
        chatListView.setContextMenu(contextMenu);
    }
    
    private void updateContextMenu(ChatMessage selectedMessage) {
        if (selectedMessage != null) {
            chatListView.getContextMenu().getItems().get(0).setDisable(false);
            chatListView.getContextMenu().getItems().get(1).setDisable(false);
        } else {
            chatListView.getContextMenu().getItems().get(0).setDisable(true);
            chatListView.getContextMenu().getItems().get(1).setDisable(true);
        }
    }

    //////////////////////////////EDIT BLOCK///////////////////////////////////
    
   private void handleEditAction() {
       
        	ChatMessage originalMessage = chatListView.getSelectionModel().getSelectedItem();

            if (originalMessage != null && originalMessage.getUser().equals(chatClient.userName)) {
                String editedText = promptUserForEdit();

                if (editedText != null) {
                    ChatMessage editedMessage = new ChatMessage(originalMessage.getUser(), editedText);
                    editedMessage.setRoomName(originalMessage.getRoomName());
                    editedMessage.setMessageType(ChatMessage.MessageType.EDIT);
                    editedMessage.setPrivateMessage(originalMessage.isPrivateMessage());
                    editedMessage.setReceiver(originalMessage.getReceiver());
                    editedMessage.setMessageId(originalMessage.getMessageId().toString());
                    editedMessage.setRoomMessage(originalMessage.isRoomMessage());
                    
                    System.out.println(originalMessage.isRoomMessage());
                    System.out.println(editedMessage.isRoomMessage());
                    
                    
                    sendEditMessageToClient(editedMessage);
                    
                    sendEditMessageToServer(editedMessage);
                   

                    
                    updateMessageInUI(originalMessage, editedMessage);
                } else {
                }
            } else {
            }
        } 
   
   private void updateMessageInUI(ChatMessage originalMessage, ChatMessage editedMessage) {
	    Platform.runLater(() -> {
	        int index = chatListView.getItems().indexOf(originalMessage);

	        if (index != -1) {
	            chatListView.getItems().set(index, editedMessage);
	            
	            chatListView.getSelectionModel().select(index);
	        } else {
	            System.out.println("Original message not found in the ListView.");
	        }
	    });
	}


    private String promptUserForEdit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Edit Message");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter the new message:");

        Optional<String> result = dialog.showAndWait();
        
        return result.map(String::trim).orElse(null);
    }


    
    private void sendEditMessageToClient(ChatMessage editMessage) {
    	if (chatClient != null) {
            chatClient.updateEditMessage(editMessage);
        } else {
            System.out.println("ChatClient instance is not available.");
    
        }
    } 
    	

    private void sendEditMessageToServer(ChatMessage editMessage) {
        if (chatClient != null) {
            chatClient.editMessage(editMessage);
        } else {
            System.out.println("ChatClient instance is not available.");
            // Handle the case where the ChatClient instance is not available
        }
    } 
    
    
    ///////////////////////////////REPLY BLOCK ////////////////////////////////////////////
    
    private void handleReplyAction() {
        ChatMessage selectedMessage = chatListView.getSelectionModel().getSelectedItem();
        if (selectedMessage != null) {
            String replyText = promptUserForReply();
            if (replyText != null) {
                ChatMessage replyMessage = new ChatMessage(chatClient.returnUsername(), replyText);
                replyMessage.setRoomName(selectedMessage.getRoomName());
                replyMessage.setMessageType(ChatMessage.MessageType.REPLY);
                replyMessage.setPrivateMessage(selectedMessage.isPrivateMessage());
                replyMessage.setReceiver(selectedMessage.getUser());
                replyMessage.setRoomMessage(selectedMessage.isRoomMessage());
                replyMessage.setMessageRepliedTo(selectedMessage);
                //replyMessage.setReplyMessage(replyMessage);
                replyMessage.setReplyId(selectedMessage.getMessageId().toString());
                
                sendReplyMessageToClient(replyMessage, selectedMessage);
                sendReplyMessageToServer(replyMessage, selectedMessage);
            }
        }
    }
    
    private String promptUserForReply() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to Message");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter your reply:");

        Optional<String> result = dialog.showAndWait();
        return result.map(String::trim).orElse(null);
    }
    
    private void sendReplyMessageToClient(ChatMessage replyMessage, ChatMessage selectedMessage) {
        if (chatClient != null) {
            chatClient.sendReplyMessage(replyMessage, selectedMessage);
        } else {
            System.out.println("ChatClient instance is not available.");
        }
    }

    private void sendReplyMessageToServer(ChatMessage replyMessage, ChatMessage selectedMessage) {
        if (chatClient != null) {
            chatClient.replyMessage(replyMessage, selectedMessage);
        } else {
            System.out.println("ChatClient instance is not available.");
        }
    }
    
    
    
    
    
    ///////////////////////////////////////////////////////////////////////////////////////
    private void displayMessageHistory() {
        List<ChatMessage> messageHistory = chatClient.getMessageHistory();
        Platform.runLater(() -> {
            for (ChatMessage newMessage : messageHistory) {
                int existingIndex = -1;

                for (int i = 0; i < chatListView.getItems().size(); i++) {
                    ChatMessage existingMessage = chatListView.getItems().get(i);
                    if (existingMessage.getMessageId().equals(newMessage.getMessageId())) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex != -1) {
                    chatListView.getItems().set(existingIndex, newMessage);
                } else {
                    chatListView.getItems().add(newMessage);
                }
            }

            chatListView.scrollTo(chatListView.getItems().size() - 1);
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
