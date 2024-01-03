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
    
    
    @Override
    public void start(Stage primaryStage) {
        // Create UI components
    	
  
        BorderPane root = new BorderPane();
        chatArea = new TextArea();
        inputField = new TextField();
        Button sendButton = new Button("Send");

        
        root.setStyle("-fx-background-color: black; -fx-text-fill: white;");
        chatListView.setStyle("-fx-control-inner-background: black;");
        inputField.setStyle("-fx-control-inner-background: black; -fx-text-fill: white;");
        sendButton.setStyle("-fx-background-color: darkgrey; -fx-text-fill: black;");
        
        // Set up the layout
        root.setCenter(chatListView);
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

        inputField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume(); 
                sendMessage();
            }
        });
        
        
          
        setContextMenu();

        
        
        // Create the scene and set it to the stage
        Scene scene = new Scene(root, 600, 450);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client");
        primaryStage.setOnCloseRequest(event -> stopChatClient());
        primaryStage.show();

        // Display the message history
       // displayMessageHistory();
        
       // setContextMenu();
       
        chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Enable/disable context menu items based on the selection
                updateContextMenu(newValue);
            }
        });
        

        new Thread(() -> {
            try {	
                chatClient.start();
                while (running) {
                    displayMessageHistory();
                    Thread.sleep(3000); // Adjust the sleep duration as needed
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
        // Enable/disable context menu items based on the selected message
        if (selectedMessage != null) {
            chatListView.getContextMenu().getItems().get(0).setDisable(false);// Enable "Edit" menu item
            chatListView.getContextMenu().getItems().get(1).setDisable(false);// Enable "Reply" menu item
        } else {
            chatListView.getContextMenu().getItems().get(0).setDisable(true);// Enable "Edit" menu item
            chatListView.getContextMenu().getItems().get(1).setDisable(true);// Disable "Reply" menu item
        }
    }

    //////////////////////////////EDIT BLOCK///////////////////////////////////
    
   private void handleEditAction() {
       
            // Retrieve the original message from the GUI or your data structure
        	ChatMessage originalMessage = chatListView.getSelectionModel().getSelectedItem();

            if (originalMessage != null) {
                // Prompt the user for the edited message
                String editedText = promptUserForEdit();

                if (editedText != null) {
                    // Create a new edited message
                    ChatMessage editedMessage = new ChatMessage(originalMessage.getUser(), editedText);
                    editedMessage.setRoomName(originalMessage.getRoomName());
                    editedMessage.setMessageType(ChatMessage.MessageType.EDIT);
                    editedMessage.setPrivateMessage(originalMessage.isPrivateMessage());
                    editedMessage.setReceiver(originalMessage.getReceiver());
                    editedMessage.setMessageId(originalMessage.getMessageId().toString());
                    editedMessage.setRoomMessage(originalMessage.isRoomMessage());
                    
                    System.out.println(originalMessage.isRoomMessage());
                    System.out.println(editedMessage.isRoomMessage());
                    // Send the edited message to the server
                    
                    
                    sendEditMessageToClient(editedMessage);
                    
                    sendEditMessageToServer(editedMessage);
                   

                    // Update the GUI to display the edited message
                    
                    updateMessageInUI(originalMessage, editedMessage);
                } else {
                    // User canceled the edit operation
                }
            } else {
                // Unable to find the original message
            }
        } 
   
   private void updateMessageInUI(ChatMessage originalMessage, ChatMessage editedMessage) {
	    Platform.runLater(() -> {
	        // Get the index of the original message in the ListView
	        int index = chatListView.getItems().indexOf(originalMessage);

	        if (index != -1) {
	            // Update the original message with the edited message	   
	            chatListView.getItems().set(index, editedMessage);
	            
	            // Optionally, you can also select the edited message in the ListView
	            chatListView.getSelectionModel().select(index);
	        } else {
	            System.out.println("Original message not found in the ListView.");
	            // Handle the case where the original message is not found in the ListView
	        }
	    });
	}


    private String promptUserForEdit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Edit Message");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter the new message:");

        // Show the dialog and wait for the user's response
        Optional<String> result = dialog.showAndWait();
        
        // If the user entered a new message, return it
        return result.map(String::trim).orElse(null);
    }


    private ChatMessage createEditMessage(String messageId, String editedMessage) {
        // Create an EDIT type ChatMessage
        ChatMessage editMessage = new ChatMessage("user", editedMessage);
        editMessage.setMessageType(ChatMessage.MessageType.EDIT);
        editMessage.setMessageId(messageId);

        return editMessage;
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

                // Check if the message is already in the ListView
                for (int i = 0; i < chatListView.getItems().size(); i++) {
                    ChatMessage existingMessage = chatListView.getItems().get(i);
                    if (existingMessage.getMessageId().equals(newMessage.getMessageId())) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex != -1) {
                    // If the message is already in the ListView, update it
                    chatListView.getItems().set(existingIndex, newMessage);
                } else {
                    // If the message is not in the ListView, add it
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
