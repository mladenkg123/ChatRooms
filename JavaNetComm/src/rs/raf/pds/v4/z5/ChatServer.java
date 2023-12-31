package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatMessage.MessageType;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.Room;
import rs.raf.pds.v4.z5.messages.WhoRequest;
import rs.raf.pds.v4.z5.messages.ListRoomsUpdate;

public class ChatServer implements Runnable,Listener{

	private volatile Thread thread = null;
	
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	
	
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();

	private List<ChatMessage> messageHistory = new ArrayList<>();
	private Map<String, List<ChatMessage>> chatRoomsMessages = new ConcurrentHashMap<>();
	Map<String, List<String>> roomInvitations = new HashMap<>();
	private Map<String, Room> chatRooms = new HashMap<>();

	
	
	public ChatServer(int portNumber) {
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		
		server.getKryo().register(ListRoomsUpdate.class);
        server.getKryo().register(ArrayList.class);

		registerListener();
	}
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login)object;
					newUserLogged(login, connection);
					connection.sendTCP(new ChatMessage(login.getUserName(), "Hello To the ChatRooms!"));
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
				    	handleRoomInvitation(connection, chatMessage);
				    } else if (chatMessage.isPrivateMessage()) {
				        sendPrivateMessage(chatMessage);
				    } else if (chatMessage.getTxt().startsWith("/join")) {
				    	handleJoinRoom(connection, chatMessage);
				    } else if (chatMessage.getTxt().startsWith("/listrooms")) {
			            listChatRooms(connection);
				    } else if (chatMessage.getMessageType() == MessageType.GET_MORE_MESSAGES) {
					        handleGetMoreMessages(chatMessage);
					        return;    
				    } else if (chatMessage.getMessageType() == MessageType.EDIT) {
				    		handleEditMessage(connection, chatMessage);
				    } else if (chatMessage.getMessageType() == MessageType.REPLY) {
			    		handleReplyMessage(connection, chatMessage);
				    } else if (chatMessage.getTxt().startsWith("/")) {
				        sendRoomMessage(connection, chatMessage);
				    } else if (object instanceof ChatMessage || object instanceof ListRoomsUpdate) {
	                    handleMessage(connection, object);
	                } else if (object instanceof WhoRequest || object instanceof InfoMessage || object instanceof ListUsers) {
	                	
				    } else {
				        System.out.println(chatMessage.getUser() + ":" + chatMessage.getTxt());
				        handleChatMessage(chatMessage, connection);
				    }
				    
				    System.out.println("++++++++++++++++++++++++++++++++");

		             System.out.println(messageHistory);
		             System.out.println(messageHistory.contains(chatMessage));

		             System.out.println("++++++++++++++++++++++++++++++++");
				    
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
	    //System.out.println(message);
	}
	
	
	
	
	 private void handleMessage(Connection connection, Object object) {
	        if (object instanceof ChatMessage) {
	            ChatMessage chatMessage = (ChatMessage) object;
	            if (chatMessage.getRoomName() != null) {
	                handleRoomMessage(connection, chatMessage);
	            } else {
	                handleChatMessage(chatMessage, connection);
	            }
	        } else if (object instanceof ListRoomsUpdate) {
	            ListRoomsUpdate listRoomsUpdate = (ListRoomsUpdate) object;
	            handleRoomUpdate(connection, listRoomsUpdate);
	        }
	    }

	 private void handleRoomMessage(Connection connection, ChatMessage chatMessage) {
		    String roomName = chatMessage.getRoomName();
		    String userName = connectionUserMap.get(connection);

		    if (chatRooms.containsKey(roomName)) {
		        chatRoomsMessages
		                .computeIfAbsent(roomName, k -> new ArrayList<>())
		                .add(chatMessage);

		        sendRoomMessage(connection, chatMessage);

		        //sendTextToUser(userName, "Your message in room '" + roomName + "' has been broadcasted.");
		    } else {
		        sendTextToUser(userName, "The room '" + roomName + "' does not exist.");
		    }
		}
	 
	 
	 

		private void handleRoomUpdate(Connection connection, ListRoomsUpdate listRoomsUpdate) {
		    // Send the updated list of rooms to all connected clients
		    for (Connection conn : userConnectionMap.values()) {
		        if (conn.isConnected() && conn != connection) {
		            conn.sendTCP(listRoomsUpdate);
		        }
		    }
		}

	

	
	// Displaying messages on the server side
	
	private void handleChatMessage(ChatMessage chatMessage, Connection connection) {
	    if (chatMessage.isPrivateMessage()) {
	        displayServerMessage("Private message from " + chatMessage.getUser() + " to " + chatMessage.getReceiver() + ": " + chatMessage.getTxt());
	    } else if (chatMessage.isRoomCreation()) {
	        displayServerMessage("Room created by " + chatMessage.getUser() +": " + chatMessage.getRoomName());
	    } else if (chatMessage.isInvitation()) {
	        displayServerMessage("Invitation from " + chatMessage.getUser() + " to join room " + chatMessage.getRoomName() +" for user " + chatMessage.getReceiver());
	    } else if (chatMessage.getRoomName() != null) {
	    	sendRoomMessage(connection, chatMessage);
	    } else {
	        displayServerMessage(chatMessage.getUser() + ": " + chatMessage.getTxt());
	    }
	    if(chatMessage.getMessageType() != ChatMessage.MessageType.EDIT || chatMessage.getMessageType() != ChatMessage.MessageType.REPLY) {
	    	addMessageToHistory(chatMessage);
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
	
	

	
	
	
	private void listChatRooms(Connection connection) {
	    StringBuilder roomList = new StringBuilder("Available chat rooms: ");
	    for (String room : chatRooms.keySet()) {
	        roomList.append(room).append(", ");
	    }
	    String rooms = roomList.substring(0, roomList.length() - 2);
	    //sendTextToUser(connectionUserMap.get(connection), rooms);

	    // Send the updated list to the requesting user
	    connection.sendTCP(new ListRoomsUpdate(new ArrayList<>(chatRooms.keySet())));
    

	}
	
	
	
	
	private void handleRoomInvitation(Connection connection, ChatMessage invitationMessage) {
	    String roomName = invitationMessage.getRoomName();
	    String sender = invitationMessage.getUser();
	    String receiver = invitationMessage.getReceiver();

	    // Add the invitation to the roomInvitations map
	    roomInvitations.computeIfAbsent(roomName, k -> new ArrayList<>()).add(receiver);

	    // Notify the sender about the successful invitation
	    sendTextToUser(sender, "Invitation to room '" + roomName + "' sent to user " + receiver);
	    System.out.println("Invitation to room '" + roomName + "' sent to user " + receiver);
	    
	    Connection receiverConnection = userConnectionMap.get(receiver);
	    if (receiverConnection != null && receiverConnection.isConnected()) {
	        receiverConnection.sendTCP(new ChatMessage(receiver, "You have been invited to join room '" + roomName + "' by user " + sender));
	    } else {
	        sendTextToUser(sender, "User " + receiver + " is not online.");
	    }
	    
	}

	private void handleJoinRoom(Connection connection, ChatMessage joinRoomMessage) {
	    String roomName = joinRoomMessage.getRoomName();
	    String userName = joinRoomMessage.getOriginalUsername();
	    String roomUserName = joinRoomMessage.getRoomUsername();
	    System.out.println("Room '" + roomName);
	    System.out.println(userName);
	    
	    if (chatRooms.containsKey(roomName)) {
	        if (roomInvitations.containsKey(roomName) && roomInvitations.get(roomName).contains(userName)) {
	            Room room = chatRooms.get(roomName);

	            room.addMember(userName);

	            roomInvitations.get(roomName).remove(userName);

	            sendTextToUser(userName, "You have successfully joined the room '" + roomName + "'.");

	            ChatMessage joinMessage = new ChatMessage("Server", userName + " has joined the room.");
	            joinMessage.setRoomName(roomName);
	            sendRoomMessage(connection, joinMessage);
	            
	            ///////poslednjih 5 poruka/////////
	            
	            if (room.getMessages().size() >= 5) {
	            	List<ChatMessage> lastMessages = room.getLastNMessages(5);
		            for (ChatMessage message : lastMessages) {
		                connection.sendTCP(message);
		                }
	            } else {
	            	
	            	int n = room.getMessages().size();
	            	List<ChatMessage> lastNMessages = room.getLastNMessages(n);
		            for (ChatMessage message : lastNMessages) {
		                connection.sendTCP(message);
	            	
		            }
	            }
	            
	        } else {
	            sendTextToUser(userName, "You are not invited to join the room '" + roomName + "'.");
	        }
	    } else {
	        sendTextToUser(userName, "The room '" + roomName + "' does not exist.");
	    }
	}

	
	
 void sendPrivateMessage(ChatMessage privateMessage) {
	    Connection receiverConnection = userConnectionMap.get(privateMessage.getReceiver());
	    Connection senderConnection = userConnectionMap.get(privateMessage.getUser());
	    String receiver = privateMessage.getReceiver();
	    if (receiverConnection != null && receiverConnection.isConnected()) {
	        receiverConnection.sendTCP(privateMessage);
	        senderConnection.sendTCP(privateMessage);
	        System.out.println("User   " + privateMessage.getUser() + "sent private message :  " + privateMessage.getTxt() + "to user :  " + privateMessage.getReceiver());
	    } else {
	        senderConnection.sendTCP(new InfoMessage("User " + receiver + " is not online."));
	        System.out.println("User " + privateMessage.getReceiver() + " is not online.");
	    }
	}
 

 private void createChatRoom(ChatMessage roomCreationMessage) {
	    String roomName = roomCreationMessage.getRoomName();
	    String creator = roomCreationMessage.getUser();

	    if (!chatRooms.containsKey(roomName)) {
	        Room room = new Room(roomName);
	        room.addMember(creator);
	        chatRooms.put(roomName, room);

	        sendTextToUser(creator, "You have successfully created the chat room '" + roomName + "'.");
	        System.out.println(creator + "have successfully created the chat room '" + roomName + "'.");
	        System.out.println(chatRooms.containsKey(roomName));

	    } else {
	        sendTextToUser(creator, "Chat room '" + roomName + "' already exists. Please choose a different name.");
	        System.out.println(creator + "Chat room '" + roomName + "' already exists. Please choose a different name.");
	    }
	}


 private void sendRoomMessage(Connection sender, ChatMessage roomMessage) {
	 	String roomName = roomMessage.getRoomName();
	    //String userName = connectionUserMap.get(sender);
	 	String userName = roomMessage.getRoomUserName();
	 	String realUsername = roomMessage.getOriginalUsername();
        System.out.println(roomMessage);
        
        

	    if (chatRooms.containsKey(roomName) && chatRooms.get(roomName).getMembers().contains(realUsername)) {
	    	
	    	Room room = chatRooms.get(roomName);
	        room.addMessage(roomMessage);
	       
	        List<String> roomMembers = chatRooms.get(roomName).getMembers();
	        System.out.println("\\\\\\\\\\\\\\\\\\");
	        System.out.println(roomMembers);
	        System.out.println(roomMessage);
	        System.out.println("\\\\\\\\\\\\\\\\\\");
	        for (String member : roomMembers) {
	            Connection memberConnection = userConnectionMap.get(member);
	            if (memberConnection != null && memberConnection.isConnected()) {
	                memberConnection.sendTCP(roomMessage);
	            }
	        }
	        
	        messageHistory.add(roomMessage);
	    } else {
	        sender.sendTCP(new InfoMessage("You are not a member of the room '" + roomName + "'."));
	    }
	}
 
 
 
 private void handleGetMoreMessages(ChatMessage getMoreMessagesMessage) {
	    String roomName = getMoreMessagesMessage.getRoomName();
	    Connection connection = userConnectionMap.get(getMoreMessagesMessage.getUser());

	    if (chatRooms.containsKey(roomName) && chatRooms.get(roomName).getMembers().contains(getMoreMessagesMessage.getUser())) {
	        Room room = chatRooms.get(roomName);
	        List<ChatMessage> lastMessages = room.getLastNMessages(20);

	        for (ChatMessage message : lastMessages) {
	            connection.sendTCP(message);
	        }
	    } else {
	        connection.sendTCP(new InfoMessage("You are not a member of the room '" + roomName + "'."));
	    }
	}
 
 
 ///////////////////////////////EDIT MESSAGE////////////
 
 private void handleEditMessage(Connection connection, ChatMessage editMessage) {
   
     ChatMessage originalMessage = findOriginalMessage(editMessage);
     System.out.println(originalMessage);
   
     if (originalMessage != null) {
         if (isUserAllowedToEdit(connection, originalMessage)) {
             
           
             
             if (originalMessage.isRoomMessage()) {
            	String roomName = originalMessage.getRoomName();
            	Room room = chatRooms.get(roomName);
            	String roomUsername = originalMessage.getRoomUserName();
            	editMessage.setRoomUsername(roomUsername);
     	        room.addMessage(editMessage);
             }
             
             
             //messageHistory.remove(originalMessage);

             //messageHistory.add(editMessage);
             
       
             
             if (originalMessage != null) {
                 if (isUserAllowedToEdit(connection, originalMessage)) {
                     // Update the original message text
                     originalMessage.setTxt(editMessage.getTxt());

                     // Update the replied message if there is one
                     updateRepliedMessage(editMessage);

                     broadcastChatMessage(originalMessage, null);
                 } else {
                     System.out.println("Cannot edit message. User not allowed.");
                 }
             } else {
                 System.out.println("Cannot edit message. Original message not found.");
             }
         

         

             System.out.println("-----------------------------");

             System.out.println(messageHistory);
             System.out.println(messageHistory.size());

             System.out.println("-----------------------------");

             broadcastChatMessage(editMessage, null);
         } else {
             System.out.println("Cannot edit message. User not allowed.");
         }
     } else {
         System.out.println("Cannot edit message. Original message not found.");
     }
 }

 private void updateRepliedMessage(ChatMessage editMessage) {
	    Queue<ChatMessage> queue = new LinkedList<>();

	    // Add the original message to the queue
	    queue.add(editMessage);

	    while (!queue.isEmpty()) {
	        ChatMessage currentMessage = queue.poll();

	        // Check if the current message has a replied message
	        if (currentMessage.getMessageRepliedTo() != null &&
	                currentMessage.getMessageRepliedTo().getMessageId().equals(editMessage.getMessageId())) {
	            // Update the replied message text
	            currentMessage.getMessageRepliedTo().setTxt(editMessage.getTxt());

	            // Add the replied message to the queue for further processing
	            queue.add(currentMessage.getMessageRepliedTo());
	        }
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
 
 
 private boolean isUserAllowedToEdit(Connection connection, ChatMessage originalMessage) {
	    String senderUsername = originalMessage.getUser();
	    String senderUsername2 = originalMessage.getOriginalUsername();
	    
	    if (senderUsername.equals(getUsernameByConnection(connection)) || 
	            senderUsername2.equals(getUsernameByConnection(connection))) {
	            return true;
	        } else {
	            return false;
	        }
	    	    
	    	   
	}
 
 
 private String getUsernameByConnection(Connection connection) {
	    return connectionUserMap.get(connection);
 	}
 
 
 ////////////////////REPLY MESSAGE///////////////////
 
 
 private void handleReplyMessage(Connection connection, ChatMessage replyMessage) {
   
    
   
    //if (selectedMessage != null) {
         
	 // Update the original message text
         //originalMessage.setTxt(editedText);
         
    	 if (replyMessage.isRoomMessage()) {
    		String username = getUsernameByConnection(connection);
    		String roomName = replyMessage.getRoomName();
         	Room room = chatRooms.get(roomName);
         	String roomUsername = username + "{"+room.getName()+"}";
         	replyMessage.setRoomUsername(roomUsername);
  	        room.addMessage(replyMessage);
          }
    	 
         messageHistory.add(replyMessage);
  
       
         broadcastChatMessage(replyMessage, null);
         
     
 }
 
////////////////////////////////////////////////////////
 


	private void sendTextToUser(String username, String text) {
	    Connection userConnection = userConnectionMap.get(username);
	    if (userConnection != null && userConnection.isConnected()) {
	        // Send a text message to the specified user
	        userConnection.sendTCP(new ChatMessage(username,text));
	        
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
		if (message.isRoomMessage()) {
	        String roomName = message.getRoomName();
	        Room room = chatRooms.get(roomName);

	        if (room != null) {
	            List<String> roomMembers = room.getMembers();

	            for (String member : roomMembers) {
	                Connection memberConnection = userConnectionMap.get(member);
	                if (memberConnection != null && memberConnection.isConnected()) {
	                    memberConnection.sendTCP(message);
	                }
	            }
	            return;
	        }
	    }

	    for (Connection conn : userConnectionMap.values()) {
	        if (conn.isConnected() && conn != exception) {
	            conn.sendTCP(message);
	        }
	    }
	}
	
	private void showTextToAll(String txt, Connection exception) {
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
