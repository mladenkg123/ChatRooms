// Updated ChatMessage class
package rs.raf.pds.v4.z5.messages;

public class ChatMessage {
    private String user;
    private String txt;
    private String receiver;
    private String roomName;
    private boolean privateMessage;
    private boolean isRoomCreation;
    private boolean isInvitation;

    protected ChatMessage() {

    }

    public ChatMessage(String user, String txt) {
        this.user = user;
        this.txt = txt;
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
}
