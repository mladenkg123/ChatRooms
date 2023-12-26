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
    private MessageType messageType;

    
    public enum MessageType {
        REGULAR, 
        GET_MORE_MESSAGES,
        EDIT
    }
    
    protected ChatMessage() {

    }

    public ChatMessage(String user, String txt) {
        this.user = user;
        this.txt = txt;
        this.messageType = MessageType.REGULAR;
        this.messageId = UUID.randomUUID().toString();
    }
    
    public UUID getMessageId() {
        return UUID.fromString(messageId);
    }
    
 
    public String getUser() {
        return user;
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

    @Override
    public String toString() {
        if(user!=null) {
            return user.toString() + ": " + txt.toString() + "\n";
        }
        else {
            return txt.toString() + "\n";
        }
    }
}
