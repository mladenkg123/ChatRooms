package rs.raf.pds.v4.z5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatMessage.MessageType;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListRoomsUpdate;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.Room;
import rs.raf.pds.v4.z5.messages.WhoRequest;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;


public class ChatClient implements Runnable{

	public static int DEFAULT_CLIENT_READ_BUFFER_SIZE = 1000000;
	public static int DEFAULT_CLIENT_WRITE_BUFFER_SIZE = 1000000;
	
	private volatile Thread thread = null;
	
	volatile boolean running = false;
	
	final Client client;
	final String hostName;
	final int portNumber;
	final String userName;
	private TextArea chatArea; 
    private List<ChatMessage> messageHistory = new ArrayList<>();
    private ListView<ChatMessage> chatListView = new ListView<>();


	
	public ChatClient(String hostName, int portNumber, String userName) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		KryoUtil.registerKryoClasses(client.getKryo());
		client.getKryo().register(ListRoomsUpdate.class); 
        client.getKryo().register(ArrayList.class); 


		registerListener();
	}
	private void registerListener() {
		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Login loginMessage = new Login(userName);
				client.sendTCP(loginMessage);
			}
			
			public void received (Connection connection, Object object) {
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					showChatMessage(chatMessage);
					return;
				}

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers)object;
					showOnlineUsers(listUsers.getUsers());
					return;
				}
				
				if (object instanceof InfoMessage) {
					InfoMessage message = (InfoMessage)object;
					showMessage("Server:"+message.getTxt());
					return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage message = (ChatMessage)object;
					showMessage(message.getUser()+"r:"+message.getTxt());
					return;
				}
				
				if (object instanceof ListRoomsUpdate) {
					ListRoomsUpdate listofRooms = (ListRoomsUpdate)object;
					
					messageHistory.add(new ChatMessage(userName, listofRooms.toString()));
					
					return;
					
				}
			}	
			
			public void disconnected(Connection connection) {
				
			}
		});
	}
	
	
	public void setChatArea(TextArea chatArea) {
        this.chatArea = chatArea;
    }
	
	
	public void sendMessage(String message) {
	    if (client.isConnected() && running) {
	        if (message.startsWith("/private")) {
	            sendPrivateMessage(message);
	        } else if (message.startsWith("/create")) {
	            createChatRoom(message);
	        } else if (message.startsWith("/invite")) {
	            sendRoomInvitation(message);
	        } else if (message.startsWith("/join")) {
	        	joinChatRoom(message);
	        } else if (message.startsWith("/listrooms")) {
	        	
	        	ChatMessage newChatMessage = new ChatMessage(userName, message);	        	
	        	client.sendTCP(newChatMessage);
	        	
	        	
	        } else if (message.startsWith("/getmoremessages")) {
	            String[] commandParts = message.split(" ", 2);
	            

	            
	            if (commandParts.length == 2) {
	                String roomName = commandParts[1];

	                ChatMessage getMoreMessagesMessage = new ChatMessage(userName, message);
	                getMoreMessagesMessage.setMessageType(MessageType.GET_MORE_MESSAGES);
	                getMoreMessagesMessage.setRoomName(roomName);

	                client.sendTCP(getMoreMessagesMessage);
	            }
	        	
	        }  else if (message.startsWith("/")) {
	            String[] commandParts = message.split(" ", 2);
	            if (commandParts.length == 2) {
	            	
	                String roomName = commandParts[0].substring(1);  
	                String roomMessageText = commandParts[1];
	                String originalUsername = userName;
	                String roomUsername = userName + "{"+ roomName +"}";

	                ChatMessage roomMessage = new ChatMessage(originalUsername, roomUsername, roomMessageText);
	                roomMessage.setRoomName(roomName);
	                roomMessage.setRoomMessage(true);

	                sendRoomMessage(roomMessage);
	            } else {
	                System.out.println("Invalid command format. Use /roomName message");
	            }
	        }  else {
	            ChatMessage chatMessage = new ChatMessage(userName, message);
	            client.sendTCP(chatMessage);
		        Platform.runLater(() -> chatArea.appendText(chatMessage.getUser() + ": " + chatMessage.getTxt() + "\n"));
		        messageHistory.add(chatMessage);
		        
	        }
	    } else {
	        System.out.println("Not connected to the server. Cannot send message.");
	    }
	}
	
	
	
	
	
	
	public void sendPrivateMessage(String message) {
	    String[] parts = message.split(" ", 3);
	    if (parts.length == 3) {
	        String receiver = parts[1];
	        String text = parts[2];
	        ChatMessage privateMessage = new ChatMessage(userName, text);
	        privateMessage.setReceiver(receiver);
	        privateMessage.setPrivateMessage(true);
	        client.sendTCP(privateMessage);
	        Platform.runLater(() -> chatArea.appendText(privateMessage.getUser() + ": " + privateMessage.getTxt() + "\n"));
	    } else {
	        System.out.println("Invalid private message format. Use /private userName message");
	    }
	}
	
	/////////////////////////////////CHAT ROOM/////////////////////////////////////////////
	
	void createChatRoom(String message) {
	    String[] parts = message.split(" ", 2);
	    if (parts.length == 2) {
	        String roomName = parts[1];
	        
	        Room room = new Room(roomName);


	        ChatMessage roomCreationMessage = new ChatMessage(userName, "");
	        roomCreationMessage.setRoomName(roomName);
	        roomCreationMessage.setRoomCreation(true);
	        client.sendTCP(roomCreationMessage);
	        System.out.println(roomName);
	    } else {
	        System.out.println("Invalid room creation format. Use /create @room_name");
	    }
	}

	public void sendRoomInvitation(String message) {
	    String[] parts = message.split(" ", 3);
	    if (parts.length == 3) {
	        ChatMessage invitationMessage = new ChatMessage(userName, "");
	        invitationMessage.setRoomName(parts[1]);
	        invitationMessage.setReceiver(parts[2]);
	        invitationMessage.setInvitation(true);
	        client.sendTCP(invitationMessage);
	    } else {
	        System.out.println("Invalid invitation format. Use /invite @room_name @username");
	    }
	}
	
	public void joinChatRoom(String message) {
	    String[] parts = message.split(" ", 2);
	    if (parts.length == 2) {
	        String roomName = parts[1];
	        String roomUsername = userName + "{"+roomName+"}";
	        ChatMessage joinRoomMessage = new ChatMessage(userName, roomUsername, "/join " + roomName);
	        joinRoomMessage.setRoomName(roomName);
	        System.out.println(roomName);

	        client.sendTCP(joinRoomMessage);
	    } else {
	        System.out.println("Invalid join room format. Use /join roomName");
	        Platform.runLater(() -> chatArea.appendText("Invalid join room format. Use /join roomName"));
	    }
	}
	
	
	
	private void sendRoomMessage(ChatMessage roomMessage) {
	    if (client.isConnected() && running) {
	        client.sendTCP(roomMessage);
	    } else {
	        System.out.println("Not connected to the server. Cannot send message.");
	    }
	}
	
	
	
	
	 List<ChatMessage> getMessageHistory() {
	        return messageHistory;
	    }
	 
	 
	 ///////////////////////EDIT MESSAGE/////////////////////////////
	 
	 public void updateEditMessage(ChatMessage editMessage) {
		    ChatMessage originalMessage = findOriginalMessage(editMessage);
		    System.out.println(editMessage.getMessageType());
		    if (originalMessage != null && userName.equals(editMessage.getUser())) {
		        if (originalMessage.getMessageType() == ChatMessage.MessageType.REPLY) {
		            String editedText = editMessage.getTxt();

		            if (editedText.contains("(Ed)")) {
		                editedText = editedText.substring(0, editedText.lastIndexOf("(Ed)"));
		            }

		            editMessage.setTxt(editedText + "\n" + "Replied to:" + originalMessage.getTxt().substring(originalMessage.getTxt().indexOf("Replied to:") + "Replied to:".length()));
		        }
		        
		     
		        if (!editMessage.getTxt().contains("(Ed)")) {
		            editMessage.setTxt(editMessage.getTxt() + "\n" + "(Ed)");
		        }
		        
		        
		        
		        List<ChatMessage> messageHistoryCopy = new ArrayList<>(messageHistory);

	             for (ChatMessage message : messageHistoryCopy) {
	                 if (message.getMessageType() == ChatMessage.MessageType.REPLY) {
	                     

	                     ChatMessage message2 = editMessage;
	                     if (message2 != null && message2.getMessageId().toString().equals(message.getReplyId())) {
	                         
	                    	 String newText = message.getTxt().replaceAll("Replied to:.*", "");
	                    	 String repliedToText = "Replied to:" + message2.getTxt().substring(0, Math.min(message2.getTxt().length(), 15));
	                    	 message.setTxt(newText + repliedToText);	                     
	                    	 }
	                 }
	             }
	            	 
	            	 
	             messageHistory = new ArrayList<>(messageHistoryCopy);
		        
		        
		        

		        messageHistory.remove(originalMessage);
		        messageHistory.add(editMessage);

		        System.out.println(messageHistory);

		    } else {
		        System.out.println("You cannot edit other users' messages!");
		    }
		}
	 
	private ChatMessage findOriginalMessage(ChatMessage Newmessage) {
	     for (ChatMessage message : messageHistory) {
	         if (message.getMessageId().equals(Newmessage.getMessageId())) {
	             return message;
	         }
	     }
	     return null;
	 }
	
	public void editMessage(ChatMessage editMessage) {
		if (client.isConnected() && running) {
	        client.sendTCP(editMessage);
	    } else {
	        System.out.println("Not connected to the server. Cannot send message.");
	    }
		
	}
	//////////////////////////////////REPLY MESSAGE////////////////////////////////////////
	
	public void sendReplyMessage(ChatMessage replyMessage, ChatMessage selectedMessage) {
		//&& userName != selectedMessage.getUser()
		
		if (selectedMessage != null) {
	         // Update the original message text			
			replyMessage.setTxt(replyMessage.getTxt() + "\n" + "Replied to:" + selectedMessage.getTxt().substring(0, Math.min(selectedMessage.getTxt().length(), 15)));
	        messageHistory.add(replyMessage);

	         
	         System.out.println(messageHistory);
	         
	     }	
	     else {
	    	 
	    	 System.out.println("You cannot edit other users message!");
	     }
	}
	
	public void replyMessage(ChatMessage replyMessage, ChatMessage selectedMessage) {
		
		if (client.isConnected() && running) {
	        client.sendTCP(replyMessage);
	        client.sendTCP(selectedMessage);
	    } else {
	        System.out.println("Not connected to the server. Cannot send message.");
	    }
		
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	
	
	
	public String returnUsername () {
		
		return userName;
		
	}
	
	public void sendJoinRoomMessage(ChatMessage joinRoomMessage) {
	    client.sendTCP(joinRoomMessage);
	}
	
	public void showChatMessage(ChatMessage chatMessage) {
	    if (chatArea != null) {
	        Platform.runLater(() -> chatArea.appendText(chatMessage.getUser() + ": " + chatMessage.getTxt() + "\n"));
	        messageHistory.add(chatMessage);
	    }
	}

	public void showMessage(String txt) {
		
		ChatMessage chatmess= new ChatMessage(userName, txt);
	    Platform.runLater(() -> chatListView.getItems().add(chatmess));
	    
	}

	public void showOnlineUsers(String[] users) {
	    if (chatArea != null) {
	        StringBuilder usersText = new StringBuilder("Server:");
	        for (int i = 0; i < users.length; i++) {
	            usersText.append(users[i]);
	            if (i != users.length - 1) {
	                usersText.append(", ");
	            }
	        }
	        Platform.runLater(() -> chatArea.appendText(usersText.toString() + "\n"));
	    }
	}

	public void start() throws IOException {
		client.start();
		connect();
		
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
	
	public void connect() throws IOException {
		client.connect(1000, hostName, portNumber);
	}
	public void run() {
		
		try (
				BufferedReader stdIn = new BufferedReader(
	                    new InputStreamReader(System.in))	// Za čitanje sa standardnog ulaza - tastature!
	        ) {
					            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput)) // userInput - tekst koji je unet sa tastature!
	            	{
	            		running = false;
	            	}
	            	else if ("WHO".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRequest());
	            	}							
	            	else {
	            		ChatMessage message = new ChatMessage(userName, userInput);
	            		client.sendTCP(message);
	            	}
	            	
	            	if (!client.isConnected() && running)
	            		connect();
	            	
	           }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			running = false;
			System.out.println("CLIENT SE DISCONNECTUJE");
			client.close();;
		}
	}
	public static void main(String[] args) {
		if (args.length != 3) {
		
            System.err.println(
                "Usage: java -jar chatClient.jar <host name> <port number> <username>");
            System.out.println("Recommended port number is 54555");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
        	ChatClient chatClient = new ChatClient(hostName, portNumber, userName);
        	chatClient.start();
        }catch(IOException e) {
        	e.printStackTrace();
        	System.err.println("Error:"+e.getMessage());
        	System.exit(-1);
        }
	}
}
