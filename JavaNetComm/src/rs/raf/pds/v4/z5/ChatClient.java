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
	private Map<String, Room> chatRooms = new HashMap<>();
	private Map<String, List<String>> roomInvitations = new HashMap<>();

	
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
	        } else if (message.startsWith("/getmoremessages")) {
	            String[] commandParts = message.split(" ", 2);
	            
                System.out.println(commandParts);

	            
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

	                ChatMessage roomMessage = new ChatMessage(userName, roomMessageText);
	                roomMessage.setRoomName(roomName);

	                sendRoomMessage(roomMessage);
	            } else {
	                System.out.println("Invalid command format. Use /roomName message");
	            }
	        }  else {
	            ChatMessage chatMessage = new ChatMessage(userName, message);
	            client.sendTCP(chatMessage);
		        Platform.runLater(() -> chatArea.appendText(chatMessage.getUser() + ": " + chatMessage.getTxt() + "\n"));
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
	        ChatMessage joinRoomMessage = new ChatMessage(userName, "/join " + roomName);
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
	
	
	
	public void sendJoinRoomMessage(ChatMessage joinRoomMessage) {
	    client.sendTCP(joinRoomMessage);
	}
	
	public void showChatMessage(ChatMessage chatMessage) {
	    if (chatArea != null) {
	        Platform.runLater(() -> chatArea.appendText(chatMessage.getUser() + ": " + chatMessage.getTxt() + "\n"));
	    }
	}

	public void showMessage(String txt) {
	    if (chatArea != null) {
	        Platform.runLater(() -> chatArea.appendText(txt + "\n"));
	    }
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
