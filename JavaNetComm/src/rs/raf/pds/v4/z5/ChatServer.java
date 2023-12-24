package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.WhoRequest;


public class ChatServer implements Runnable{

	private volatile Thread thread = null;
	
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	
	private List<ChatMessage> messageHistory = new ArrayList<>();
	
	public ChatServer(int portNumber) {
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login)object;
					newUserLogged(login, connection);
					connection.sendTCP(new InfoMessage("Hello "+login.getUserName()));
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
				
				if (object instanceof ChatMessage) {
				    ChatMessage chatMessage = (ChatMessage) object;
				    if (chatMessage.isRoomCreation()) {
				        createChatRoom(chatMessage);
				    } else if (chatMessage.isInvitation()) {
				        sendRoomInvitation(chatMessage);
				    } else if (chatMessage.isPrivateMessage()) {
				        sendPrivateMessage(chatMessage);
				    } else {
				        System.out.println(chatMessage.getUser() + ":" + chatMessage.getTxt());
				        handleChatMessage(chatMessage, connection);
				    }
				    return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
				
				if (object instanceof ListUsers || object instanceof WhoRequest || object instanceof InfoMessage) {
			        handleOtherMessages(object);
			        return;
			    }
															
			}
			
			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user+" has disconnected!", connection);
			}
		});
	}
	
	
	private void displayServerMessage(String message) {
	    System.out.println(message);
	}
	
	
	
	// Displaying messages on the server side
	
	private void handleChatMessage(ChatMessage chatMessage, Connection connection) {
	    if (chatMessage.isPrivateMessage()) {
	        displayServerMessage("Private message from " + chatMessage.getUser() +
	                " to " + chatMessage.getReceiver() + ": " + chatMessage.getTxt());
	    } else if (chatMessage.isRoomCreation()) {
	        displayServerMessage("Room created by " + chatMessage.getUser() +
	                ": " + chatMessage.getRoomName());
	    } else if (chatMessage.isInvitation()) {
	        displayServerMessage("Invitation from " + chatMessage.getUser() +
	                " to join room " + chatMessage.getRoomName() +
	                " for user " + chatMessage.getReceiver());
	    } else {
	        displayServerMessage(chatMessage.getUser() + ": " + chatMessage.getTxt());
	    }

	    broadcastChatMessage(chatMessage, connection);
	}

	private void handleOtherMessages(Object object) {
	    if (object instanceof InfoMessage) {
	        InfoMessage infoMessage = (InfoMessage) object;
	        displayServerMessage("Server: " + infoMessage.getTxt());
	    } else if (object instanceof ListUsers) {
	        ListUsers listUsers = (ListUsers) object;
	        displayServerMessage("Online users: " + String.join(", ", listUsers.getUsers()));
	    } else if (object instanceof WhoRequest) {
	    }
	}
	
	
	
	
 void sendPrivateMessage(ChatMessage privateMessage) {
	    Connection receiverConnection = userConnectionMap.get(privateMessage.getReceiver());
	    Connection senderConnection = userConnectionMap.get(privateMessage.getUser());
	    String receiver = privateMessage.getReceiver();
	    if (receiverConnection != null && receiverConnection.isConnected()) {
	        receiverConnection.sendTCP(privateMessage);
	        System.out.println("User   " + privateMessage.getUser() + "sent private message :  " + privateMessage.getTxt() + "to user :  " + privateMessage.getReceiver());
	    } else {
	        senderConnection.sendTCP(new InfoMessage("User " + receiver + " is not online."));
	        System.out.println("User " + privateMessage.getReceiver() + " is not online.");
	    }
	}
	
	private Map<String, List<String>> chatRooms = new ConcurrentHashMap<>();  // Map to store chat rooms and their members

	private void createChatRoom(ChatMessage roomCreationMessage) {
	    String roomName = roomCreationMessage.getRoomName();
	    String creator = roomCreationMessage.getUser();

	    if (!chatRooms.containsKey(roomName)) {
	        // Create a new chat room
	        chatRooms.put(roomName, new ArrayList<>());
	        chatRooms.get(roomName).add(creator);

	        // Notify the creator about the successful creation
	        sendTextToUser(creator, "You have successfully created the chat room '" + roomName + "'.");
	        System.out.println(creator + "have successfully created the chat room '" + roomName + "'.");
	    } else {
	        // Chat room with the same name already exists
	        sendTextToUser(creator, "Chat room '" + roomName + "' already exists. Please choose a different name.");
	        System.out.println(creator + "Chat room '" + roomName + "' already exists. Please choose a different name.");
	    }
	}

	private void sendRoomInvitation(ChatMessage invitationMessage) {
	    String roomName = invitationMessage.getRoomName();
	    String inviter = invitationMessage.getUser();
	    String invitee = invitationMessage.getReceiver();

	    if (chatRooms.containsKey(roomName)) {
	        // Check if the inviter is a member of the chat room
	        if (chatRooms.get(roomName).contains(inviter)) {
	            // Send an invitation to the specified user
	            Connection inviteeConnection = userConnectionMap.get(invitee);
	            if (inviteeConnection != null && inviteeConnection.isConnected()) {
	                sendTextToUser(invitee, "You have been invited to join the chat room '" + roomName +
	                        "' by " + inviter + ". Type '/join " + roomName + "' to join.");
	                System.out.println(invitee + "You have been invited to join the chat room '" + roomName +
	                        "' by " + inviter + ". Type '/join " + roomName + "' to join.");
	                
	            } else {
	                sendTextToUser(inviter, "User '" + invitee + "' is not online.");
	                System.out.println(inviter + "User '" + invitee + "' is not online.");
	            }
	        } else {
	            sendTextToUser(inviter, "You are not a member of the chat room '" + roomName + "'.");
	            System.out.println(inviter + "You are not a member of the chat room '" + roomName + "'.");
	        }
	    } else {
	        sendTextToUser(inviter, "Chat room '" + roomName + "' does not exist.");
            System.out.println(inviter + "Chat room '" + roomName + "' does not exist.");
	    }
	}

	private void sendTextToUser(String username, String text) {
	    Connection userConnection = userConnectionMap.get(username);
	    if (userConnection != null && userConnection.isConnected()) {
	        // Send a text message to the specified user
	        userConnection.sendTCP(new InfoMessage(text));
	    }
	}
	
	
	public void addMessageToHistory(ChatMessage message) {
        messageHistory.add(message);
    }
	
	public List<ChatMessage> getMessageHistory() {
	        return new ArrayList<>(messageHistory);
	    }
	
	String[] getAllUsers() {
		String[] users = new String[userConnectionMap.size()];
		int i=0;
		for (String user: userConnectionMap.keySet()) {
			users[i] = user;
			i++;
		}
		
		return users;
	}
	void newUserLogged(Login loginMessage, Connection conn) {
		userConnectionMap.put(loginMessage.getUserName(), conn);
		connectionUserMap.put(conn, loginMessage.getUserName());
		showTextToAll("User "+loginMessage.getUserName()+" has connected!", conn);
	}
	private void broadcastChatMessage(ChatMessage message, Connection exception) {
		for (Connection conn: userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(message);
		}
	}
	private void showTextToAll(String txt, Connection exception) {
		System.out.println(txt);
		for (Connection conn: userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(new InfoMessage(txt));
		}
	}
	public void start() throws IOException {
		server.start();
		server.bind(portNumber);
		
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
	@Override
	public void run() {
		running = true;
		
		while(running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
	        System.err.println("Usage: java -jar chatServer.jar <port number>");
	        System.out.println("Recommended port number is 54555");
	        System.exit(1);
	   }
	    
	   int portNumber = Integer.parseInt(args[0]);
	   try { 
		   ChatServer chatServer = new ChatServer(portNumber);
	   	   chatServer.start();
	   
			chatServer.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	   }
	}
	
   
   
}
