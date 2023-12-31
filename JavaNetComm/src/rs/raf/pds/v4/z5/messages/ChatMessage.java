// Updated ChatMessage class
package rs.raf.pds.v4.z5.messages;
import java.util.UUID;

public class ChatMessage {
	
    private String user;
    private String txt;
    private String receiver;
    private String roomName;
    private String messageId;
    private boolean privateMessage;
    private boolean roomMessage;
    private boolean isRoomCreation;
    private boolean isInvitation;
    private ChatMessage MessageRepliedTo = null;
    private MessageType messageType;
    private String originalUsername; 
    private String roomUsername;

    
    
	public enum MessageType {
        REGULAR, 
        GET_MORE_MESSAGES,
        EDIT,
        REPLY 
    }
    
    protected ChatMessage() {

    }

    public ChatMessage(String user, String txt) {
        this.user = user;
        this.txt = txt;
        this.messageType = MessageType.REGULAR;
        this.messageId = UUID.randomUUID().toString();
    }
    
    public ChatMessage(String originalUsername, String roomUsername, String txt) {
    	this.messageId = UUID.randomUUID().toString();
    	this.messageType = MessageType.REGULAR;
    	this.user = originalUsername;
        this.originalUsername = originalUsername;
        this.roomUsername = roomUsername;
        this.txt = txt;
        
    }

    
    public UUID getMessageId() {
    	
        return UUID.fromString(messageId);
    }
    
    public ChatMessage getMessageRepliedTo() {
		return MessageRepliedTo;
	}

	public void setMessageRepliedTo(ChatMessage messageRepliedTo) {
		MessageRepliedTo = messageRepliedTo;
	}
 
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
		this.user = user;
	}

	public void setTxt(String txt) {
		this.txt = txt;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getTxt() {
        return txt;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isPrivateMessage() {
        return privateMessage;
    }

    public void setPrivateMessage(boolean privateMessage) {
        this.privateMessage = privateMessage;
    }
    
    
    public boolean isRoomMessage() {
        return roomMessage;
    }

    public void setRoomMessage(boolean roomMessage) {
        this.roomMessage = roomMessage;
    }
    

    public boolean isRoomCreation() {
        return isRoomCreation;
    }

    public void setRoomCreation(boolean isRoomCreation) {
        this.isRoomCreation = isRoomCreation;
    }

    public boolean isInvitation() {
        return isInvitation;
    }

    public void setInvitation(boolean isInvitation) {
        this.isInvitation = isInvitation;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getOriginalUsername() {
        return originalUsername;
    }
    
    public String getRoomUserName() {
        return roomUsername;
    }
    
    
    public String getRoomUsername() {
		return roomUsername;
	}

	public void setRoomUsername(String roomUsername) {
		this.roomUsername = roomUsername;
	}

	public void setOriginalUsername(String originalUsername) {
		this.originalUsername = originalUsername;
	}
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ChatMessage message = (ChatMessage) obj;
        return this.getMessageId().equals(message.getMessageId());
    }
	
	

	//"Message ID: " + messageId.toString() + "\n"
    @Override
    public String toString() {
    	
    	if (isRoomMessage()) {
    		return roomUsername + ":" + txt.toString() +"\n";
    	}
    	
        if(user!=null) {
        	return user.toString() + ": " + txt.toString() + "\n";
        }
        else {
            return txt.toString() + "\n";
        }
    }
}
